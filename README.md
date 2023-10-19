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

## Component, mutation, resolver breif overview

### Mutation `mutations/sign-up`:

 * Purpose: It registers a new user.
 * Action: Upon initiation, it removes any existing errors related to the signup form.
 * Remote: It indicates that this mutation should also run remotely (i.e., on the server side).
 * Result Action: Once the remote mutation finishes and a result is received, this code:
  ** Checks if there's an error and, if so, shows it.
  ** If no error, sets user info, goes to the todos page, clears the sign-up form, and loads todos.

### Component `SignUpForm`:

 * Purpose: It displays a sign-up form.
 * Query: Specifies which data the component needs from the app state.
 * Render: Contains the UI code, where:
  ** There's a display of error if any.
  ** User inputs for "User Name" and "Password".
  ** A button to submit the form (i.e., sign up).
  ** A link to go to the sign-in page.

### Resolver `user-info-resolver`:

 * Purpose: Resolves user info based on a token.
 * Input: Nothing specific is needed.
 * Output: Information about a user.
 * Logic: It extracts a token, decodes the user's name from the token, fetches the user ID based on the user name, and finally constructs user info.

### Mutation `sign-up` for Pathom:

 * Purpose: Server-side logic for registering a user.
 * Logic:
  ** It attempts to add a user with the given username and password.
  ** If there's an error, it returns the error.
  ** If successful, it augments the server's response to include a token cookie.

### Here's a higher-level summary:

 * Mutations handle actions and state changes. The `mutations/sign-up` mutation manages user registration.
 * Components define UI and how data is fetched. The `SignUpForm` component displays the sign-up form.
 * Resolvers provide a way to fetch specific data in Pathom, and in this case, the `user-info-resolver` fetches user information.
 * There's also a server-side mutation (`sign-up`), showing the logic behind user registration on the backend.


## Development

To run the test:

```bash
clj -X:test
```

To run the repl:

```bash
clj -A:dev
```
