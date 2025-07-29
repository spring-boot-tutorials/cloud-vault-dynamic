Resources:
- https://developer.hashicorp.com/vault/tutorials/app-integration/spring-reload-secrets
- https://www.youtube.com/watch?v=E9XDfOVNN2U&t=3s
- https://spring.io/projects/spring-cloud-vault

# Create Docker Network
Create docker network 
```shell
docker network create my-app-network
```

# Run Database (MariaDB)
```shell
docker run \
  --name my-mysql-server \
  --network my-app-network \
  -e MYSQL_ROOT_PASSWORD=my_root_password \
  -e MYSQL_DATABASE=my_app_database \
  -p 3306:3306 \
  mysql:latest
```

Get IP address of container
```shell
docker ps
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <CONTAINER_NAME_OR_ID>
```

# Run Vault Server
```shell
docker run \
  --cap-add=IPC_LOCK \
  --name=my-vault-server \
  --network my-app-network \
  -e 'VAULT_DEV_ROOT_TOKEN_ID=my-root-token' \
  -e 'VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:8200' \
  -p 8200:8200 \
  hashicorp/vault
```
- `--cap-add=IPC_LOCK`: This capability is crucial to prevent sensitive information from being swapped to disk, enhancing security.
- `--name=dev-vault`: Assigns a name to the container for easier management.
- `hashicorp/vault`: Specifies the official Docker image for HashiCorp Vault.
- `VAULT_DEV_ROOT_TOKEN_ID`: Sets the ID of the initial root token.
- `VAULT_DEV_LISTEN_ADDRESS`: Sets the IP and port for the listener (defaults to 0.0.0.0:8200).
- `-p 8200:8200`: Maps port 8200 from the container to port 8200 on the host, allowing access to the Vault UI or API.

# Setup Secrets on Vault Server (UI Way)

Login to Vault Server:
- token way:
- token: `my-root-token`

Enable database
- goto: http://localhost:8200/ui/vault/settings/mount-secret-backend
- click `Enable Engine`

Create database connection:
- goto: http://localhost:8200/ui/vault/secrets/database/create
- fill in form:
  - database_plugin: `mysql-aurora-database-plugin`
  - connection_name: `my-mysql`
  - connection_url: `root:my_root_password@tcp(172.19.0.2:3306)/`
    - replace `172.19.0.2` with IP address of `mysql-server`
  - username: `root`
  - password: `my_root_password`
- click `create` and `disable rotate`

Create role:
- goto: http://localhost:8200/ui/vault/secrets/database/create?itemType=role
- fill in form:
  - role_name: `my-app-role`
  - db_name: `my-mysql`
  - role_type: `dynamic`
  - Generated credentials’s Time-to-Live (TTL): `10s`
  - Generated credentials’s maximum Time-to-Live (Max TTL): `10s`
  - creation_statements:
    - `CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';`
    - `GRANT ALL PRIVILEGES ON my_app_database.* TO '{{name}}'@'%' WITH GRANT OPTION;`
    - `FLUSH PRIVILEGES;`
  - revoke_statements:
    - TODO REMOVE OLD USER

Manually Generate credentials:
- goto: http://localhost:8200/ui/vault/secrets/database/show/role/my-app-role?type=dynamic
- click on `Generate Credentials`


# Setup Secrets on Vault Server (Terminal Way TODO)

Connect to Vault Server
```shell
docker ps
docker exec -it CONTAINER_ID /bin/sh
```

Configure `vault` command
```shell
export VAULT_ADDR="http://127.0.0.1:8200"
export VAULT_TOKEN="my-root-token"
```

Create Database Connection
- DON'T EXECUTE THIS COMMAND BUT DO THIS ON UI:
- Make sure to update IP of `connection_url`
```shell
vault secrets enable database
vault write database/config/my-database \
      plugin_name=mysql-database-plugin \
      connection_url="root:my_root_password@tcp(172.19.0.2:3306)/" \
      allowed_roles="my-role" \
      username="root" \
      password="my_root_password"
```

Create Role
- DON'T EXECUTE THIS COMMAND BUT DO THIS ON UI: http://localhost:8200/ui/vault/secrets/database/create?itemType=role
```shell
vault write database/roles/my-app-role \
      db_name='my-mysql' \
      creation_statements="CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';, GRANT ALL PRIVILEGES ON my_app_database.* TO '{{name}}'@'%' WITH GRANT OPTION;, FLUSH PRIVILEGES;" \
      default_ttl="1h" \
      max_ttl="24h"
```

Generate Credentials
```shell
vault read database/creds/my-app-role
```
