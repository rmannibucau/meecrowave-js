response.getWriter().write(JSON.stringify({
    uri: request.getRequestURI()
}));
