# Todo application

## Run the application

Run a terminal and execute following script:

```bash
npx shadow-cljs watch main
```

Run another terminal and execute following script:

```bash
clj -A:dev -X server.core/start
```

Then go to `http://localhost:3000/index.html`


## Development

To run the test:

```bash
clj -X:test
```

To run the repl:

```bash
clj -A:dev
```
