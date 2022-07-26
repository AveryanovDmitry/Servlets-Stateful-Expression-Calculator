package servlets;

import
        java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import bsh.EvalError;
import bsh.Interpreter;

@WebServlet("/calc/*")
public class StatefulExpressionCalculatorServlet extends HttpServlet {
    private static final long serialVersionUID = 1;
    private static final String ATTRIBUTE_NAME_EXPRESSION = "expression";
    private static final int ANSWER_CODE_200 = 200;
    private static final int ANSWER_CODE_201 = 201;
    private static final int ANSWER_CODE_204 = 204;
    private static final int ERROR_409 = 409;
    private static final int INDEX_FROM_BEGINNING_URI = 6;
    private static final String HEADER_VALUE  = "Location ";
    private static final String URL_EXPRESSION = "/calc/expression";
    private static final String URL_CALC = "/calc/";
    private static final Pattern REGEX_FOR_DELIMITER = Pattern.compile("\"\\\\A\"");
    private static final String STATEMENT_RESULT = "result = ";
    private static final String RESULT_STR = "result";
    private static final char FIRST_ALPHABETICAL_SYMBOL_LOW = 'a';
    private static final char LAST_ALPHABETICAL_SYMBOL_LOW = 'z';
    private String BUFFER_FOR_RESULT_INPUT_STREAM = "";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String result = processExpressionCalculate(req, resp);
        if (result != null) {
            PrintWriter writer = resp.getWriter();
            writer.write(result);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String paramOfUri = req.getRequestURI().substring(INDEX_FROM_BEGINNING_URI);
        if (paramOfUri.equals(ATTRIBUTE_NAME_EXPRESSION)) {
            processExpression(req, resp);
        } else {
            processVariable(req, resp, paramOfUri.charAt(0));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String paramOfUri = req.getRequestURI().substring(INDEX_FROM_BEGINNING_URI);
        HttpSession session = req.getSession();
        session.removeAttribute(paramOfUri);
        resp.setStatus(ANSWER_CODE_204);
    }

    private String interpreterEval(StringBuilder expressionBuilder) {
        Interpreter interpreter = new Interpreter();
        Integer result = null;
        try {
            interpreter.eval (STATEMENT_RESULT + expressionBuilder);
            result = (Integer) interpreter.get(RESULT_STR);
        } catch (EvalError e) {
            e.printStackTrace();
        }
        return Integer.toString(result);
    }

    private String processExpressionCalculate (HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String expressionSessionAttribute = (String) session.getAttribute(ATTRIBUTE_NAME_EXPRESSION);
        StringBuilder expressionBuilder = new StringBuilder();
        for (int i = 0; i < expressionSessionAttribute.length(); ++i) {
            char c = expressionSessionAttribute.charAt(i);
            if (c >= FIRST_ALPHABETICAL_SYMBOL_LOW && c <= LAST_ALPHABETICAL_SYMBOL_LOW) {
                String attributeValue = (String) session.getAttribute(String.valueOf(c));
                if (attributeValue == null) {
                    resp.sendError(ERROR_409);
                    return null;
                }
                char value = attributeValue.charAt(0);
                if (value >= FIRST_ALPHABETICAL_SYMBOL_LOW && value <= LAST_ALPHABETICAL_SYMBOL_LOW) {
                    expressionBuilder.append(session.getAttribute(attributeValue));
                } else {
                    expressionBuilder.append(attributeValue);
                }
            } else {
                expressionBuilder.append(c);
            }
        }
        return interpreterEval(expressionBuilder);
    }

    private void processExpression(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String expression = inputStreamToString(req.getInputStream());
        if (session.getAttribute(ATTRIBUTE_NAME_EXPRESSION) == null) {
            resp.setStatus(ANSWER_CODE_201);
            resp.addHeader(HEADER_VALUE, URL_EXPRESSION);
        } else {
            resp.setStatus(ANSWER_CODE_200);
        }
        session.setAttribute(ATTRIBUTE_NAME_EXPRESSION, expression);
    }

    private void processVariable(HttpServletRequest req, HttpServletResponse resp, char variableName)
            throws IOException {
        HttpSession session = req.getSession();
        String variableValue = inputStreamToString(req.getInputStream());
        if (session.getAttribute(String.valueOf(variableName)) == null) {
            resp.setStatus(ANSWER_CODE_201);
            resp.addHeader(HEADER_VALUE, URL_CALC + variableName);
        } else {
            resp.setStatus(ANSWER_CODE_200);
        }
        session.setAttribute(String.valueOf(variableName), variableValue);
    }

    private String inputStreamToString(InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            if (scanner.hasNext()) {
                BUFFER_FOR_RESULT_INPUT_STREAM = scanner.useDelimiter(REGEX_FOR_DELIMITER).next();
            }
            return BUFFER_FOR_RESULT_INPUT_STREAM;
        }
    }
}
