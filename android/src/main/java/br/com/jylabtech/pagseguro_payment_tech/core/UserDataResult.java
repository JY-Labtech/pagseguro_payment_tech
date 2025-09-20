package br.com.jylabtech.pagseguro_payment_tech.core;

public class UserDataResult extends  BaseResult {
    private String method;
    private String address;
    private String city;
    private String cnpjCpf;
    private String addressComplement;
    private String companyName;
    private String userNickName;
    private String addressState;
    private String email;

    public void setMethod(String value) {
        this.method = value;
    }

    public void setAddress(String value) {
        this.address = value;
    }

    public void setCity(String value) {
        this.city = value;
    }

    public void setCnpjCpf(String value) {
        this.cnpjCpf = value;
    }

    public void setAddressComplement(String value) {
        this.addressComplement = value;
    }

    public void setCompanyName(String value) {
        this.companyName = value;
    }
    public void setUserNickName(String value) {
        this.userNickName = value;
    }

    public void setAddressState(String value) {
        this.addressState = value;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public String getMethod() {return this.method;}

    public String getAddress() {return this.address;}
    public String getCity() {return this.city;}
    public String getCnpjCpf(){return this.cnpjCpf;}
    public String getAddressComplement() {return this.addressComplement;}
    public String getCompanyName(){return this.companyName;}
    public String getUserNickName(){return this.userNickName;}
    public String getAddressState(){return this.addressState;}
    public String getEmail(){return this.email;}
}
