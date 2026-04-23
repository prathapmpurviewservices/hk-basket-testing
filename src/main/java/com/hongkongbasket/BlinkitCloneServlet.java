package com.hongkongbasket;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

@WebServlet(urlPatterns = {"/", "/login", "/signup", "/logout", "/checkout", "/images/*"})
public class BlinkitCloneServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getRequestPath(req);

        switch (path) {
            case "/":
                writeHtml(resp, BlinkitCloneServer.renderHomePage(ProductStore.loadProducts(), req.getContextPath()));
                break;
            case "/login":
                writeHtml(resp, BlinkitCloneServer.renderLoginPage());
                break;
            case "/signup":
                writeHtml(resp, BlinkitCloneServer.renderSignupPage());
                break;
            case "/logout":
                writeHtml(resp, renderLogoutPage());
                break;
            default:
                if (path.startsWith("/images/")) {
                    serveImage(resp, path.substring("/images/".length()));
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getRequestPath(req);

        switch (path) {
            case "/signup":
                handleSignup(req, resp);
                break;
            case "/login":
                handleLogin(req, resp);
                break;
            case "/checkout":
                handleCheckout(req, resp);
                break;
            default:
                resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                break;
        }
    }

    private void handleSignup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (isBlank(fullName) || isBlank(email) || isBlank(password)) {
            writeHtml(resp, BlinkitCloneServer.renderSignupPage("All fields are required"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!password.equals(confirmPassword)) {
            writeHtml(resp, BlinkitCloneServer.renderSignupPage("Passwords do not match"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (UserStore.userExists(email)) {
            writeHtml(resp, BlinkitCloneServer.renderSignupPage("Email already registered"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        User user = UserStore.registerUser(email, password, fullName);
        String sessionId = UserStore.createSession(user.getUserId());
        addSessionCookie(resp, sessionId);
        writeHtml(resp, BlinkitCloneServer.renderSignupSuccess(user, sessionId));
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (isBlank(email) || isBlank(password)) {
            writeHtml(resp, BlinkitCloneServer.renderLoginPage("Email and password are required"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        User user = UserStore.loginUser(email, password);
        if (user == null) {
            writeHtml(resp, BlinkitCloneServer.renderLoginPage("Invalid email or password"));
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String sessionId = UserStore.createSession(user.getUserId());
        addSessionCookie(resp, sessionId);
        writeHtml(resp, BlinkitCloneServer.renderLoginSuccess(user, sessionId));
    }

    private void handleCheckout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String customer = defaultString(req.getParameter("customerName"), "Customer");
        String card = defaultString(req.getParameter("cardNumber"), "");
        String order = defaultString(req.getParameter("orderDetails"), "No order details.");
        String total = defaultString(req.getParameter("orderTotal"), "0.00");
        String paymentMethod = defaultString(req.getParameter("paymentMethod"), "HK Online Pay");

        writeHtml(resp, BlinkitCloneServer.renderCheckoutPage(customer, card, order, total, paymentMethod));
    }

    private void serveImage(HttpServletResponse resp, String imageName) throws IOException {
        if (imageName == null || imageName.isBlank() || imageName.contains("..")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String resourcePath = "images/" + imageName;
        try (InputStream inputStream = BlinkitCloneServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType;
            if (imageName.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else if (imageName.endsWith(".png")) {
                contentType = "image/png";
            } else if (imageName.endsWith(".jpg") || imageName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else {
                contentType = "application/octet-stream";
            }

            resp.setContentType(contentType);
            try (var out = resp.getOutputStream()) {
                inputStream.transferTo(out);
            }
        }
    }

    private String renderLogoutPage() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta http-equiv=\"refresh\" content=\"2;url=/\">\n" +
                "  <title>Logout - Hong Kong Basket</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <p>You have been logged out. Redirecting...<p>\n" +
                "</body>\n" +
                "</html>";
    }

    private void addSessionCookie(HttpServletResponse resp, String sessionId) {
        Cookie cookie = new Cookie("sessionId", sessionId);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        resp.addCookie(cookie);
    }

    private String getRequestPath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String path = (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
        if (path.isEmpty()) {
            return "/";
        }
        return path;
    }

    private void writeHtml(HttpServletResponse resp, String html) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(html);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
