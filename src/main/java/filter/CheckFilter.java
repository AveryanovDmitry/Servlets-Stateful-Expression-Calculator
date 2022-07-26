package filter;

import utils.ReadServletRequest;
import utils.State;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class CheckFilter implements Filter {
    private static final int ERROR_400 = 400;
    private static final int ERROR_403 = 403;
    private static final int ERROR_204 = 204;
    private static final int MAX_VALUE_VAR = 10000;
    private static final int MIN_VALUE_VAR = -10000;
    private static final Pattern REGEX_URI = Pattern.compile("/calc/([a-z])$");
    private static final Pattern REGEX_FOR_DELIMITER = Pattern.compile("\\A");
    private static final String URL_EXPRESSION = "/calc/expression";
    private static final String URL_RESULT = "/calc/result";
    private static final String WRONG_CHARACTER_EXPRESSION_MESSAGE = "wrong input character expression format";
    private static final String WRONG_SIZE_VALUE_VARIABLE_MESSAGE = "value variable too small or too big";
    private static final String WRONG_VALUE_URI_MESSAGE = "Incorrect value of URI: ";
    private static String bufferForInputStream = "";
    private State state;
    private FilterConfig filterConfig;


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        ReadServletRequest wrappedRequest = new ReadServletRequest((HttpServletRequest)request);
        final String uri = ((HttpServletRequest) request).getRequestURI();
        if (URL_EXPRESSION.equals(uri)) {
            if (checkingExpressionForInvalidCharacters(inputStreamToString(wrappedRequest.getInputStream()))) {
                chain.doFilter(wrappedRequest, resp);
            } else {
                resp.sendError(ERROR_400, WRONG_CHARACTER_EXPRESSION_MESSAGE);
            }
        } else if (REGEX_URI.matcher(uri).find()) {
            if (checkVariableForValue(wrappedRequest, resp)) {
                chain.doFilter(wrappedRequest, resp);
            } else {
                resp.sendError(ERROR_403, WRONG_SIZE_VALUE_VARIABLE_MESSAGE);
            }
        } else if (URL_RESULT.equals(uri)) {
            chain.doFilter(wrappedRequest, resp);
        } else {
            resp.sendError(ERROR_400, WRONG_VALUE_URI_MESSAGE + uri);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static boolean checkVariableForValue(ServletRequest request, HttpServletResponse resp)
            throws IOException {
        boolean result = true;
        try {
            int valueVariableFromRequest = Integer.parseInt(inputStreamToString(request.getInputStream()));
            if (valueVariableFromRequest < MIN_VALUE_VAR || valueVariableFromRequest > MAX_VALUE_VAR) {
                result = false;
            }
        } catch (NumberFormatException e) {
            resp.sendError(ERROR_204, e.getMessage());
        }
        return result;
    }

    private static String inputStreamToString(InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            if (scanner.hasNext()) {
                bufferForInputStream = scanner.useDelimiter(REGEX_FOR_DELIMITER).next();
            }
            return bufferForInputStream;
        }
    }

    public boolean checkingExpressionForInvalidCharacters(String expression) {
        boolean result = true;
        state = State.INITIAL;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == ' ') {
                continue;
            }
            if (state == State.INITIAL || state == State.OPEN_BRACKET
                    || state == State.OPERATION) {
                checkInitial(c);
            } else {
                if (checkSymbol(c) == null) {
                    result = false;
                }
            }
        }
        return result;
    }

    private void checkInitial(char c) {
        if (c == '(') {
            state = State.OPEN_BRACKET;
        } else if (c >= 'a' && c <= 'z') {
            state = State.CHARACTER;
        } else if (c >= '0' && c <= '9') {
            state = State.DIGIT;
        } else {
            state = State.SIGN;
        }
    }

    private State checkSymbol(char c) {
        if (c == '-' || c == '+' || c == '*' || c == '/') {
            state = State.OPERATION;
        } else if (c == ')') {
            state = State.CLOSE_BRACKET;
        } else if (c >= '0' && c <= '9') {
            state = State.DIGIT;
        } else {
            state = null;
        }
        return state;
    }

    @Override
    public void destroy() {
        this.filterConfig = null;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
}
