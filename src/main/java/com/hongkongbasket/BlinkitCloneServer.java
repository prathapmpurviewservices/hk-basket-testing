package com.hongkongbasket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlinkitCloneServer {
    public static void main(String[] args) throws IOException {
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null && !portEnv.isEmpty()) ? Integer.parseInt(portEnv) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/signup", new SignupHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/checkout", new CheckoutHandler());
        server.createContext("/images", new ImageHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Hong Kong Basket server started on port " + port);
    }

    static class ImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path == null || !path.startsWith("/images/")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String fileName = path.substring("/images/".length());
            if (fileName.isBlank() || fileName.contains("..")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String resourcePath = "images/" + fileName;
            try (InputStream inputStream = BlinkitCloneServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] data = inputStream.readAllBytes();
                String contentType;
                if (fileName.endsWith(".svg")) {
                    contentType = "image/svg+xml";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else {
                    contentType = "application/octet-stream";
                }
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            }
        }
    }

    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            List<Product> products = ProductStore.loadProducts();
            String html = renderHomePage(products, "");
            writeResponse(exchange, 200, html);
        }
    }

    static class CheckoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Map<String, String> params = parseForm(exchange.getRequestBody());
            String customer = params.getOrDefault("customerName", "Customer");
            String card = params.getOrDefault("cardNumber", "");
            String order = params.getOrDefault("orderDetails", "No order details.");
            String total = params.getOrDefault("orderTotal", "0.00");
            String paymentMethod = params.getOrDefault("paymentMethod", "HK Online Pay");
            String html = renderCheckoutPage(customer, card, order, total, paymentMethod);
            writeResponse(exchange, 200, html);
        }

        private Map<String, String> parseForm(InputStream body) throws IOException {
            String form = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = new HashMap<>();
            for (String entry : form.split("&")) {
                if (entry.isBlank()) {
                    continue;
                }
                String[] pair = entry.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                params.put(name, value);
            }
            return params;
        }
    }

    private static void writeResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    static String renderHomePage(List<Product> products, String contextPath) {
        String cards = products.stream()
                .map(product -> renderProductCard(product, contextPath))
                .collect(Collectors.joining("\n"));
        String categoryButtons = renderCategoryButtons(products);

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">\n" +
                "  <title>Hong-Kong-Basket</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; }\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f3f7fb; color: #1f2937; }\n" +
                "    header { background: #006d75; color: white; padding: 16px 16px; }\n" +
                "    header h1 { margin: 0 0 4px; font-size: 1.8rem; }\n" +
                "    header p { margin: 0; opacity: 0.88; font-size: 0.85rem; }\n" +
                "    .container { max-width: 1200px; margin: 16px auto; padding: 0 12px; }\n" +
                "    .topbar { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }\n" +
                "    .topbar .label { font-size: 0.85rem; color: #374151; }\n" +
                "    .categories { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; overflow-x: auto; -webkit-overflow-scrolling: touch; }\n" +
                "    .category-button { border: 1px solid #d1d5db; background: white; color: #374151; border-radius: 999px; padding: 8px 14px; cursor: pointer; transition: all 0.2s ease; font-size: 0.85rem; white-space: nowrap; min-height: 36px; display: flex; align-items: center; }\n" +
                "    .category-button.active, .category-button:hover { background: #06b6d4; border-color: #06b6d4; color: white; }\n" +
                "    .content { display: grid; grid-template-columns: 1fr; gap: 20px; }\n" +
                "    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }\n" +
                "    .product-card { background: white; border-radius: 16px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08); padding: 14px; border: 1px solid #e5e7eb; }\n" +
                "    .product-card .product-image { width: 100%; height: 120px; object-fit: cover; border-radius: 10px; margin-bottom: 10px; background: #f8fafc; }\n" +
                "    .product-card h2 { margin: 0 0 6px; font-size: 0.95rem; line-height: 1.3; }\n" +
                "    .product-card .category { font-size: 0.75rem; color: #64748b; margin-bottom: 8px; }\n" +
                "    .product-card .price { color: #0f766e; font-weight: 700; font-size: 1rem; margin: 8px 0; }\n" +
                "    .product-card p { margin: 0 0 12px; color: #475569; line-height: 1.4; font-size: 0.8rem; }\n" +
                "    .add-button { display: flex; align-items: center; justify-content: center; background: #0f766e; color: white; border: none; border-radius: 10px; padding: 10px 12px; cursor: pointer; font-weight: 600; transition: background 0.2s ease; font-size: 0.85rem; min-height: 40px; width: 100%; }\n" +
                "    .add-button:hover { background: #115e59; }\n" +
                "    .add-button:active { transform: scale(0.98); }\n" +
                "    .cart-panel { background: white; border-radius: 16px; border: 1px solid #e5e7eb; padding: 16px; box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06); }\n" +
                "    .cart-panel h2 { margin: 0 0 12px; font-size: 1.1rem; }\n" +
                "    .cart-items { min-height: 180px; margin-bottom: 12px; color: #475569; font-size: 0.9rem; }\n" +
                "    .cart-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #e5e7eb; gap: 8px; }\n" +
                "    .cart-item:last-child { border-bottom: none; }\n" +
                "    .cart-item button { border: none; background: transparent; color: #ef4444; cursor: pointer; padding: 4px 8px; font-size: 0.8rem; min-height: 36px; }\n" +
                "    .cart-total { display: flex; justify-content: space-between; align-items: center; font-size: 1rem; font-weight: 700; margin-bottom: 14px; }\n" +
                "    .checkout-button { width: 100%; padding: 12px 14px; border: none; border-radius: 10px; background: #0f766e; color: white; font-size: 0.95rem; font-weight: 700; cursor: pointer; transition: background 0.2s ease; min-height: 44px; }\n" +
                "    .checkout-button:disabled { opacity: 0.55; cursor: not-allowed; }\n" +
                "    .checkout-button:active { transform: scale(0.98); }\n" +
                "    .footer { text-align: center; margin: 32px 0 16px; color: #64748b; font-size: 0.8rem; padding: 0 12px; }\n" +
                "    .modal { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); display: flex; align-items: center; justify-content: center; padding: 12px; z-index: 100; overflow-y: auto; }\n" +
                "    .modal.hidden { display: none; }\n" +
                "    .modal-content { background: white; border-radius: 16px; width: min(500px, 95vw); padding: 16px; position: relative; max-height: 90vh; overflow-y: auto; }\n" +
                "    .modal-content h2 { margin-top: 0; margin-bottom: 14px; font-size: 1.2rem; }\n" +
                "    .close-button { position: absolute; top: 12px; right: 12px; border: none; background: transparent; font-size: 1.8rem; cursor: pointer; color: #64748b; padding: 8px 12px; border-radius: 6px; transition: all 0.2s ease; min-height: 44px; min-width: 44px; display: flex; align-items: center; justify-content: center; }\n" +
                "    .close-button:active { background: #f1f5f9; }\n" +
                "    .form-group { margin-bottom: 12px; }\n" +
                "    .form-group label { display: block; margin-bottom: 6px; font-size: 0.85rem; color: #334155; font-weight: 500; }\n" +
                "    .form-group input, .form-group select { width: 100%; padding: 12px 12px; border-radius: 10px; border: 1px solid #cbd5e1; font-size: 0.95rem; box-sizing: border-box; min-height: 44px; }\n" +
                "    .order-summary { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 12px; padding: 12px; margin-bottom: 14px; white-space: pre-wrap; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace; font-size: 0.8rem; line-height: 1.4; max-height: 150px; overflow-y: auto; }\n" +
                "    .button-group { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }\n" +
                "    .cancel-button { flex: 1; padding: 12px 14px; border: 1px solid #cbd5e1; border-radius: 10px; background: white; color: #475569; font-size: 0.95rem; font-weight: 700; cursor: pointer; transition: all 0.2s ease; min-height: 44px; }\n" +
                "    .cancel-button:active { background: #f8fafc; }\n" +
                "    .header-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }\n" +
                "    .auth-buttons { display: flex; gap: 8px; flex-wrap: wrap; }\n" +
                "    .auth-buttons a { color: white; text-decoration: none; padding: 8px 12px; border-radius: 6px; font-size: 0.8rem; transition: background 0.2s ease; min-height: 40px; display: flex; align-items: center; white-space: nowrap; }\n" +
                "    .auth-buttons a.signin { background: rgba(255, 255, 255, 0.2); }\n" +
                "    .auth-buttons a.signin:active { background: rgba(255, 255, 255, 0.3); }\n" +
                "    .auth-buttons a.signup { background: white; color: #006d75; font-weight: 700; }\n" +
                "    .auth-buttons a.signup:active { background: #e8f7f8; }\n" +
                "    .user-info { color: white; font-size: 0.85rem; }\n" +
                "    .user-info a { color: white; text-decoration: underline; }\n" +
                "    @media (min-width: 768px) {\n" +
                "      header { padding: 20px 32px; }\n" +
                "      header h1 { font-size: 2.2rem; }\n" +
                "      header p { font-size: 0.95rem; }\n" +
                "      .container { padding: 0 24px; margin: 24px auto; }\n" +
                "      .topbar { margin-bottom: 18px; }\n" +
                "      .topbar .label { font-size: 0.95rem; }\n" +
                "      .categories { gap: 10px; margin-bottom: 24px; }\n" +
                "      .category-button { padding: 10px 16px; font-size: 0.9rem; }\n" +
                "      .content { grid-template-columns: 1.8fr 1fr; gap: 24px; }\n" +
                "      .grid { grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }\n" +
                "      .product-card { padding: 16px; border-radius: 18px; }\n" +
                "      .product-card .product-image { height: 140px; margin-bottom: 12px; }\n" +
                "      .product-card h2 { font-size: 1.05rem; }\n" +
                "      .product-card .price { font-size: 1.1rem; }\n" +
                "      .add-button { font-size: 0.9rem; padding: 11px 14px; }\n" +
                "      .cart-panel { padding: 20px; }\n" +
                "      .cart-panel h2 { font-size: 1.2rem; }\n" +
                "      .cart-items { font-size: 0.95rem; }\n" +
                "      .product-card p { font-size: 0.85rem; }\n" +
                "      .button-group { flex-direction: row; }\n" +
                "      .cancel-button { flex: 1; }\n" +
                "      .checkout-button { font-size: 1rem; }\n" +
                "    }\n" +
                "    @media (min-width: 1024px) {\n" +
                "      header { padding: 26px 32px; }\n" +
                "      header h1 { font-size: 2.4rem; }\n" +
                "      .grid { grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; }\n" +
                "      .product-card { padding: 20px; }\n" +
                "      .product-card .product-image { height: 150px; }\n" +
                "      .product-card h2 { font-size: 1.15rem; }\n" +
                "      .product-card p { font-size: 0.9rem; }\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header>\n" +
                "    <div class=\"header-top\">\n" +
                "      <div>\n" +
                "        <h1 style=\"margin: 0 0 4px; color: white;\">Hong-Kong-Basket</h1>\n" +
                "        <p style=\"margin: 0; opacity: 0.88;\">Fresh vegetables and grocery delivered to your home.</p>\n" +
                "      </div>\n" +
                "      <div class=\"auth-buttons\">\n" +
                "        <a href=\"/login\" class=\"signin\">Sign In</a>\n" +
                "        <a href=\"/signup\" class=\"signup\">Sign Up</a>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </header>\n" +
                "  <main class=\"container\">\n" +
                "    <div class=\"topbar\">\n" +
                "      <div class=\"label\">Shop by Category</div>\n" +
                "      <div class=\"label\">Please do not provide any Bank/Card details. This is for testing/Learning only </div>\n" +
                "    </div>\n" +
                "    <div class=\"categories\">\n" + categoryButtons +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <section>\n" +
                "        <h2>All Products</h2>\n" +
                "        <div class=\"grid\" id=\"product-grid\">\n" + cards +
                "        </div>\n" +
                "      </section>\n" +
                "      <aside class=\"cart-panel\">\n" +
                "        <h2>Your Cart</h2>\n" +
                "        <div class=\"cart-items\" id=\"cart-items\">Your cart is empty</div>\n" +
                "        <div class=\"cart-total\">\n" +
                "          <span>Total</span>\n" +
                "          <span id=\"cart-total\">HK$ 0.00</span>\n" +
                "        </div>\n" +
                "        <button id=\"checkout-button\" class=\"checkout-button\" disabled>Checkout</button>\n" +
                "      </aside>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "  <div class=\"footer\">Prices shown in Hong Kong Dollars (HKD). Experience checkout with a simulated payment gateway.</div>\n" +
                "  <div id=\"checkout-dialog\" class=\"modal hidden\">\n" +
                "    <div class=\"modal-content\">\n" +
                "      <button class=\"close-button\" type=\"button\" onclick=\"closeCheckout()\">×</button>\n" +
                "      <h2>Secure Payment</h2>\n" +
                "      <div class=\"order-summary\" id=\"checkout-summary\">Preparing your order...</div>\n" +
                "      <form id=\"checkout-form\">\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"customerName\">Name on card</label>\n" +
                "          <input id=\"customerName\" name=\"customerName\" required placeholder=\"Hong Kong Basket Customer\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"paymentMethod\">Payment gateway</label>\n" +
                "          <select id=\"paymentMethod\" name=\"paymentMethod\">\n" +
                "            <option>HK Online Pay</option>\n" +
                "            <option>UnionPay Secure</option>\n" +
                "            <option>Global Pay</option>\n" +
                "          </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"cardNumber\">Card number</label>\n" +
                "          <input id=\"cardNumber\" name=\"cardNumber\" required placeholder=\"4242 4242 4242 4242\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"expiry\">Expiry</label>\n" +
                "          <input id=\"expiry\" name=\"expiry\" required placeholder=\"MM/YY\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"cvv\">CVV</label>\n" +
                "          <input id=\"cvv\" name=\"cvv\" required placeholder=\"123\">\n" +
                "        </div>\n" +
                "        <input type=\"hidden\" id=\"order-details\" name=\"orderDetails\">\n" +
                "        <input type=\"hidden\" id=\"order-total\" name=\"orderTotal\">\n" +
                "        <div class=\"button-group\">\n" +
                "          <button type=\"button\" class=\"cancel-button\" onclick=\"closeCheckout()\">Cancel</button>\n" +
                "          <button type=\"submit\" class=\"checkout-button\">Pay HK$ <span id=\"checkout-amount\">0.00</span></button>\n" +
                "        </div>\n" +
                "      </form>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    const categoryButtons = document.querySelectorAll('.category-button');\n" +
                "    const productCards = document.querySelectorAll('.product-card');\n" +
                "    const cartItemsElement = document.getElementById('cart-items');\n" +
                "    const cartTotalValue = document.getElementById('cart-total');\n" +
                "    const checkoutButton = document.getElementById('checkout-button');\n" +
                "    const checkoutDialog = document.getElementById('checkout-dialog');\n" +
                "    const checkoutForm = document.getElementById('checkout-form');\n" +
                "    const checkoutSummary = document.getElementById('checkout-summary');\n" +
                "    const checkoutAmount = document.getElementById('checkout-amount');\n" +
                "    const orderDetailsInput = document.getElementById('order-details');\n" +
                "    const orderTotalInput = document.getElementById('order-total');\n" +
                "    let cart = [];\n" +
                "    function filterCategory(category) {\n" +
                "      productCards.forEach(card => {\n" +
                "        const matches = category === 'All' || card.dataset.category === category;\n" +
                "        card.style.display = matches ? 'block' : 'none';\n" +
                "      });\n" +
                "    }\n" +
                "    categoryButtons.forEach(button => {\n" +
                "      button.addEventListener('click', event => {\n" +
                "        event.preventDefault();\n" +
                "        categoryButtons.forEach(btn => btn.classList.remove('active'));\n" +
                "        button.classList.add('active');\n" +
                "        filterCategory(button.dataset.category);\n" +
                "      });\n" +
                "    });\n" +
                "    document.querySelectorAll('.add-button').forEach(button => {\n" +
                "      button.addEventListener('click', () => {\n" +
                "        const item = {\n" +
                "          id: button.dataset.id,\n" +
                "          name: button.dataset.name,\n" +
                "          price: Number(button.dataset.price),\n" +
                "          quantity: 1\n" +
                "        };\n" +
                "        const existing = cart.find(entry => entry.id === item.id);\n" +
                "        if (existing) { existing.quantity += 1; } else { cart.push(item); }\n" +
                "        renderCart();\n" +
                "      });\n" +
                "    });\n" +
                "    function renderCart() {\n" +
                "      if (cart.length === 0) {\n" +
                "        cartItemsElement.textContent = 'Your cart is empty';\n" +
                "        cartTotalValue.textContent = 'HK$ 0.00';\n" +
                "        checkoutButton.disabled = true;\n" +
                "        return;\n" +
                "      }\n" +
                "      cartItemsElement.innerHTML = '';\n" +
                "      let total = 0;\n" +
                "      cart.forEach(item => {\n" +
                "        const lineTotal = item.quantity * item.price;\n" +
                "        total += lineTotal;\n" +
                "        const row = document.createElement('div');\n" +
                "        row.className = 'cart-item';\n" +
                "        row.innerHTML = `<div>${item.quantity}× ${item.name}</div><div>${formatPrice(lineTotal)} <button data-id='${item.id}'>Remove</button></div>`;\n" +
                "        row.querySelector('button').addEventListener('click', () => {\n" +
                "          cart = cart.filter(entry => entry.id !== item.id);\n" +
                "          renderCart();\n" +
                "        });\n" +
                "        cartItemsElement.appendChild(row);\n" +
                "      });\n" +
                "      cartTotalValue.textContent = formatPrice(total);\n" +
                "      checkoutButton.disabled = false;\n" +
                "    }\n" +
                "    function formatPrice(value) {\n" +
                "      return 'HK$ ' + value.toFixed(2);\n" +
                "    }\n" +
                "    checkoutButton.addEventListener('click', () => {\n" +
                "      openCheckout();\n" +
                "    });\n" +
                "    checkoutDialog.addEventListener('click', (e) => {\n" +
                "      if (e.target === checkoutDialog) {\n" +
                "        closeCheckout();\n" +
                "      }\n" +
                "    });\n" +
                "    function openCheckout() {\n" +
                "      const items = cart.map(item => `${item.quantity}× ${item.name} - ${formatPrice(item.quantity * item.price)}`).join('\\n');\n" +
                "      const total = cart.reduce((sum, item) => sum + item.quantity * item.price, 0);\n" +
                "      checkoutSummary.textContent = `Order details:\n${items}\n\nTotal: ${formatPrice(total)}\n\nPayment gateway: HK Online Pay`;\n" +
                "      orderDetailsInput.value = items;\n" +
                "      orderTotalInput.value = total.toFixed(2);\n" +
                "      checkoutAmount.textContent = total.toFixed(2);\n" +
                "      checkoutDialog.classList.remove('hidden');\n" +
                "    }\n" +
                "    function closeCheckout() {\n" +
                "      checkoutDialog.classList.add('hidden');\n" +
                "    }\n" +
                "    checkoutForm.addEventListener('submit', event => {\n" +
                "      event.preventDefault();\n" +
                "      const formData = new URLSearchParams(new FormData(checkoutForm));\n" +
                "      fetch('/checkout', { method: 'POST', body: formData })\n" +
                "        .then(response => response.text())\n" +
                "        .then(html => {\n" +
                "          document.open();\n" +
                "          document.write(html);\n" +
                "          document.close();\n" +
                "        });\n" +
                "    });\n" +
                "    window.closeCheckout = closeCheckout;\n" +
                "    renderCart();\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    static String renderCategoryButtons(List<Product> products) {
        List<String> categories = products.stream()
                .map(Product::getCategory)
                .distinct()
                .collect(Collectors.toList());

        StringBuilder builder = new StringBuilder();
        builder.append("<button type=\"button\" class=\"category-button active\" data-category=\"All\">All</button>\n");
        for (String category : categories) {
            builder.append("<button type=\"button\" class=\"category-button\" data-category=\"")
                    .append(escapeHtml(category))
                    .append("\">")
                    .append(escapeHtml(category))
                    .append("</button>\n");
        }
        return builder.toString();
    }

    static String renderProductCard(Product product, String contextPath) {
        return "<article class=\"product-card\" data-category=\"" + escapeHtml(product.getCategory()) + "\">" +
                "<img class=\"product-image\" src=\"" + escapeHtml(contextPath + product.getImageUrl()) + "\" alt=\"" + escapeHtml(product.getName()) + "\" loading=\"lazy\" onerror=\"this.onerror=null;this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgZmlsbD0iI2Y4ZmFmZiIvPjx0ZXh0IHg9IjEwMCIgeT0iNzUiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzMzMzMzMyIHRleHQtYW5jaG9yPSJtaWRkbGUiPkltYWdlPC90ZXh0Pjwvc3ZnPg==';\" />" +
                "<div class=\"category\">" + escapeHtml(product.getCategory()) + "</div>" +
                "<h2>" + escapeHtml(product.getName()) + "</h2>" +
                "<div class=\"price\">HK$ " + product.getPriceHkd() + ".00</div>" +
                "<p>" + escapeHtml(product.getDescription()) + "</p>" +
                "<button type=\"button\" class=\"add-button\" data-id=\"" + escapeHtml(product.getId()) + "\" data-name=\"" + escapeHtml(product.getName()) + "\" data-price=\"" + product.getPriceHkd() + "\">Add to Cart</button>" +
                "</article>";
    }

    static String renderCheckoutPage(String customer, String card, String order, String total, String paymentMethod) {
        String maskedCard = card.length() > 4 ? "**** **** **** " + card.substring(card.length() - 4) : escapeHtml(card);
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Payment Complete - Hong-Kong-Basket</title>\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #eef2ff; color: #111827; }\n" +
                "    .container { max-width: 700px; margin: 60px auto; padding: 0 24px; }\n" +
                "    .card { background: white; border-radius: 24px; padding: 32px; box-shadow: 0 24px 70px rgba(15, 23, 42, 0.12); }\n" +
                "    h1 { margin-top: 0; color: #0f172a; }\n" +
                "    .status { color: #16a34a; font-size: 1.05rem; margin: 12px 0 24px; }\n" +
                "    .detail { margin-bottom: 18px; }\n" +
                "    .detail strong { display: block; margin-bottom: 6px; color: #334155; }\n" +
                "    .summary { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 18px; padding: 18px; white-space: pre-wrap; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace; }\n" +
                "    .button { display: inline-flex; align-items: center; justify-content: center; margin-top: 24px; padding: 14px 20px; background: #2563eb; color: white; text-decoration: none; border-radius: 14px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <main class=\"container\">\n" +
                "    <div class=\"card\">\n" +
                "      <h1>Payment Successful</h1>\n" +
                "      <div class=\"status\">Thank you, " + escapeHtml(customer) + ". Your order is confirmed.</div>\n" +
                "      <div class=\"detail\"><strong>Payment Gateway</strong>" + escapeHtml(paymentMethod) + "</div>\n" +
                "      <div class=\"detail\"><strong>Payment Method</strong>Card ending " + escapeHtml(maskedCard) + "</div>\n" +
                "      <div class=\"detail\"><strong>Order Total</strong>HK$ " + escapeHtml(total) + "</div>\n" +
                "      <div class=\"summary\">" + escapeHtml(order) + "</div>\n" +
                "      <a class=\"button\" href=\"/\">Continue Shopping</a>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "</body>\n" +
                "</html>";
    }

    static class SignupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String html = renderSignupPage();
                writeResponse(exchange, 200, html);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseForm(exchange.getRequestBody());
                String fullName = params.getOrDefault("fullName", "").trim();
                String email = params.getOrDefault("email", "").trim();
                String password = params.getOrDefault("password", "").trim();
                String confirmPassword = params.getOrDefault("confirmPassword", "").trim();

                if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    String html = renderSignupPage("All fields are required");
                    writeResponse(exchange, 400, html);
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    String html = renderSignupPage("Passwords do not match");
                    writeResponse(exchange, 400, html);
                    return;
                }

                if (UserStore.userExists(email)) {
                    String html = renderSignupPage("Email already registered");
                    writeResponse(exchange, 400, html);
                    return;
                }

                User user = UserStore.registerUser(email, password, fullName);
                String sessionId = UserStore.createSession(user.getUserId());
                String html = renderSignupSuccess(user, sessionId);
                writeResponse(exchange, 200, html);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private Map<String, String> parseForm(InputStream body) throws IOException {
            String form = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = new HashMap<>();
            for (String entry : form.split("&")) {
                if (entry.isBlank()) continue;
                String[] pair = entry.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                params.put(name, value);
            }
            return params;
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String html = renderLoginPage();
                writeResponse(exchange, 200, html);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseForm(exchange.getRequestBody());
                String email = params.getOrDefault("email", "").trim();
                String password = params.getOrDefault("password", "").trim();

                if (email.isEmpty() || password.isEmpty()) {
                    String html = renderLoginPage("Email and password are required");
                    writeResponse(exchange, 400, html);
                    return;
                }

                User user = UserStore.loginUser(email, password);
                if (user == null) {
                    String html = renderLoginPage("Invalid email or password");
                    writeResponse(exchange, 401, html);
                    return;
                }

                String sessionId = UserStore.createSession(user.getUserId());
                String html = renderLoginSuccess(user, sessionId);
                writeResponse(exchange, 200, html);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private Map<String, String> parseForm(InputStream body) throws IOException {
            String form = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = new HashMap<>();
            for (String entry : form.split("&")) {
                if (entry.isBlank()) continue;
                String[] pair = entry.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                params.put(name, value);
            }
            return params;
        }
    }

    static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\">\n" +
                    "  <meta name=\"refresh\" content=\"2;url=/\">\n" +
                    "  <title>Logout - Hong Kong Basket</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <p>You have been logged out. Redirecting...</p>\n" +
                    "</body>\n" +
                    "</html>";
            writeResponse(exchange, 200, html);
        }
    }

    static String renderSignupPage() {
        return renderSignupPage(null);
    }

    static String renderSignupPage(String error) {
        String errorHtml = error != null ? "<div style=\"color: #dc2626; margin-bottom: 16px; padding: 12px; background: #fee2e2; border-radius: 8px; font-size: 0.9rem;\">" + escapeHtml(error) + "</div>" : "";
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">\n" +
                "  <title>Sign Up - Hong Kong Basket</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; }\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f3f7fb; color: #1f2937; }\n" +
                "    header { background: #006d75; color: white; padding: 16px 16px; }\n" +
                "    header h1 { margin: 0; font-size: 1.5rem; }\n" +
                "    .container { max-width: 400px; margin: 40px auto; padding: 0 16px; }\n" +
                "    .card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06); border: 1px solid #e5e7eb; }\n" +
                "    h1 { margin: 0 0 16px; text-align: center; font-size: 1.4rem; color: #0f172a; }\n" +
                "    .form-group { margin-bottom: 16px; }\n" +
                "    label { display: block; margin-bottom: 6px; font-size: 0.85rem; color: #334155; font-weight: 500; }\n" +
                "    input { width: 100%; padding: 12px 12px; border-radius: 10px; border: 1px solid #cbd5e1; font-size: 0.95rem; box-sizing: border-box; min-height: 44px; }\n" +
                "    button { width: 100%; padding: 12px 16px; background: #0f766e; color: white; border: none; border-radius: 10px; font-size: 0.95rem; font-weight: 700; cursor: pointer; transition: background 0.2s ease; min-height: 44px; }\n" +
                "    button:active { background: #115e59; transform: scale(0.98); }\n" +
                "    .link { text-align: center; margin-top: 16px; font-size: 0.85rem; }\n" +
                "    .link a { color: #0f766e; text-decoration: none; }\n" +
                "    .link a:active { text-decoration: underline; }\n" +
                "    @media (min-width: 640px) {\n" +
                "      header { padding: 20px 32px; }\n" +
                "      header h1 { font-size: 1.8rem; }\n" +
                "      .container { margin: 60px auto; padding: 0 24px; }\n" +
                "      .card { padding: 32px; border-radius: 20px; }\n" +
                "      h1 { font-size: 1.6rem; margin-bottom: 20px; }\n" +
                "      .form-group { margin-bottom: 18px; }\n" +
                "      label { font-size: 0.9rem; margin-bottom: 8px; }\n" +
                "      input { padding: 12px 14px; font-size: 0.95rem; }\n" +
                "      button { font-size: 1rem; }\n" +
                "      .link { font-size: 0.9rem; }\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header>\n" +
                "    <h1>Hong-Kong-Basket</h1>\n" +
                "  </header>\n" +
                "  <main class=\"container\">\n" +
                "    <div class=\"card\">\n" +
                "      <h1>Create Account</h1>\n" +
                "      " + errorHtml + "\n" +
                "      <form method=\"POST\" action=\"/signup\">\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"fullName\">Full Name</label>\n" +
                "          <input id=\"fullName\" name=\"fullName\" type=\"text\" required placeholder=\"John Doe\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"email\">Email</label>\n" +
                "          <input id=\"email\" name=\"email\" type=\"email\" required placeholder=\"john@example.com\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"password\">Password</label>\n" +
                "          <input id=\"password\" name=\"password\" type=\"password\" required placeholder=\"••••••••\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"confirmPassword\">Confirm Password</label>\n" +
                "          <input id=\"confirmPassword\" name=\"confirmPassword\" type=\"password\" required placeholder=\"••••••••\">\n" +
                "        </div>\n" +
                "        <button type=\"submit\">Sign Up</button>\n" +
                "      </form>\n" +
                "      <div class=\"link\">Already have an account? <a href=\"/login\">Sign In</a></div>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "</body>\n" +
                "</html>";
    }

    static String renderLoginPage() {
        return renderLoginPage(null);
    }

    static String renderLoginPage(String error) {
        String errorHtml = error != null ? "<div style=\"color: #dc2626; margin-bottom: 16px; padding: 12px; background: #fee2e2; border-radius: 8px; font-size: 0.9rem;\">" + escapeHtml(error) + "</div>" : "";
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">\n" +
                "  <title>Sign In - Hong Kong Basket</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; }\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f3f7fb; color: #1f2937; }\n" +
                "    header { background: #006d75; color: white; padding: 16px 16px; }\n" +
                "    header h1 { margin: 0; font-size: 1.5rem; }\n" +
                "    .container { max-width: 400px; margin: 40px auto; padding: 0 16px; }\n" +
                "    .card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06); border: 1px solid #e5e7eb; }\n" +
                "    h1 { margin: 0 0 16px; text-align: center; font-size: 1.4rem; color: #0f172a; }\n" +
                "    .form-group { margin-bottom: 16px; }\n" +
                "    label { display: block; margin-bottom: 6px; font-size: 0.85rem; color: #334155; font-weight: 500; }\n" +
                "    input { width: 100%; padding: 12px 12px; border-radius: 10px; border: 1px solid #cbd5e1; font-size: 0.95rem; box-sizing: border-box; min-height: 44px; }\n" +
                "    button { width: 100%; padding: 12px 16px; background: #0f766e; color: white; border: none; border-radius: 10px; font-size: 0.95rem; font-weight: 700; cursor: pointer; transition: background 0.2s ease; min-height: 44px; }\n" +
                "    button:active { background: #115e59; transform: scale(0.98); }\n" +
                "    .link { text-align: center; margin-top: 16px; font-size: 0.85rem; }\n" +
                "    .link a { color: #0f766e; text-decoration: none; }\n" +
                "    .link a:active { text-decoration: underline; }\n" +
                "    @media (min-width: 640px) {\n" +
                "      header { padding: 20px 32px; }\n" +
                "      header h1 { font-size: 1.8rem; }\n" +
                "      .container { margin: 60px auto; padding: 0 24px; }\n" +
                "      .card { padding: 32px; border-radius: 20px; }\n" +
                "      h1 { font-size: 1.6rem; margin-bottom: 20px; }\n" +
                "      .form-group { margin-bottom: 18px; }\n" +
                "      label { font-size: 0.9rem; margin-bottom: 8px; }\n" +
                "      input { padding: 12px 14px; font-size: 0.95rem; }\n" +
                "      button { font-size: 1rem; }\n" +
                "      .link { font-size: 0.9rem; }\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header>\n" +
                "    <h1>Hong-Kong-Basket</h1>\n" +
                "  </header>\n" +
                "  <main class=\"container\">\n" +
                "    <div class=\"card\">\n" +
                "      <h1>Sign In</h1>\n" +
                "      " + errorHtml + "\n" +
                "      <form method=\"POST\" action=\"/login\">\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"email\">Email</label>\n" +
                "          <input id=\"email\" name=\"email\" type=\"email\" required placeholder=\"john@example.com\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label for=\"password\">Password</label>\n" +
                "          <input id=\"password\" name=\"password\" type=\"password\" required placeholder=\"••••••••\">\n" +
                "        </div>\n" +
                "        <button type=\"submit\">Sign In</button>\n" +
                "      </form>\n" +
                "      <div class=\"link\">Don't have an account? <a href=\"/signup\">Sign Up</a></div>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "</body>\n" +
                "</html>";
    }

    static String renderSignupSuccess(User user, String sessionId) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"refresh\" content=\"2;url=/\">\n" +
                "  <title>Welcome - Hong Kong Basket</title>\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f3f7fb; }\n" +
                "    .container { max-width: 500px; margin: 100px auto; padding: 0 24px; text-align: center; }\n" +
                "    .success { background: white; border-radius: 20px; padding: 40px; box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06); }\n" +
                "    h1 { color: #16a34a; margin: 0 0 16px; }\n" +
                "    p { color: #475569; margin: 0 0 24px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"success\">\n" +
                "      <h1>Welcome, " + escapeHtml(user.getFullName()) + "!</h1>\n" +
                "      <p>Your account has been created. Redirecting to shopping...</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    document.cookie = 'sessionId=" + sessionId + "; path=/; max-age=86400';\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    static String renderLoginSuccess(User user, String sessionId) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"refresh\" content=\"2;url=/\">\n" +
                "  <title>Welcome Back - Hong Kong Basket</title>\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f3f7fb; }\n" +
                "    .container { max-width: 500px; margin: 100px auto; padding: 0 24px; text-align: center; }\n" +
                "    .success { background: white; border-radius: 20px; padding: 40px; box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06); }\n" +
                "    h1 { color: #16a34a; margin: 0 0 16px; }\n" +
                "    p { color: #475569; margin: 0 0 24px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"success\">\n" +
                "      <h1>Welcome Back, " + escapeHtml(user.getFullName()) + "!</h1>\n" +
                "      <p>You have been signed in. Redirecting to shopping...</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    document.cookie = 'sessionId=" + sessionId + "; path=/; max-age=86400';\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    static String escapeHtml(String value) {

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
