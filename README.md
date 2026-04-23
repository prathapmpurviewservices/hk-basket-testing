# Hong-Kong-Basket

A lightweight Java web application inspired by Blinkit. This Maven-only app serves a grocery storefront with HKD pricing, cart checkout, and a simulated payment gateway.

## Build

```bash
mvn package
```

## Run

```bash
java -jar target/hong-kong-basket-1.0-SNAPSHOT.jar
```

Then open `http://localhost:8080` in your browser.

## Features

- Browsable grocery items by category
- Cart support with item quantity and removal
- Secure checkout page with payment gateway simulation
- Prices shown in Hong Kong Dollars (HKD)
- No external dependencies; uses Java built-in `com.sun.net.httpserver.HttpServer`
