package pl.netia.tests.ttapi.qa.support;

public enum Tenant {

    ALPHA("alpha", "Test1234!"),
    BETA("beta", "Test1234!"),
    GAMMA("gamma", "Test1234!");

    private final String username;
    private final String password;

    Tenant(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}
