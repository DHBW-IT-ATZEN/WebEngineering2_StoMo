# Local TLS certificates (git-ignored)

The HTTPS override (`docker-compose.https.yml`) mounts this directory into nginx at
`/etc/nginx/certs` and expects a locally-trusted certificate for `stomo.dev`.

Generate it once with [mkcert](https://github.com/FiloSottile/mkcert):

```bash
mkcert -install      # one-time: install a local trusted CA
mkcert stomo.dev     # run inside this folder → creates stomo.dev.pem + stomo.dev-key.pem
```

Then add `127.0.0.1 stomo.dev` to your hosts file and start the stack with the HTTPS override
(see the project README, "Option C").

The `*.pem` files here are git-ignored on purpose — **never commit private keys.**
