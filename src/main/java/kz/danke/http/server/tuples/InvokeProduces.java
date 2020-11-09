package kz.danke.http.server.tuples;

public class InvokeProduces {

    private Object invokeResult;
    private String contentTypeProduces;

    public InvokeProduces() {
    }

    public InvokeProduces(Object invokeResult, String contentTypeProduces) {
        this.invokeResult = invokeResult;
        this.contentTypeProduces = contentTypeProduces;
    }

    public Object getInvokeResult() {
        return invokeResult;
    }

    public void setInvokeResult(Object invokeResult) {
        this.invokeResult = invokeResult;
    }

    public String getContentTypeProduces() {
        return contentTypeProduces;
    }

    public void setContentTypeProduces(String contentTypeProduces) {
        this.contentTypeProduces = contentTypeProduces;
    }
}
