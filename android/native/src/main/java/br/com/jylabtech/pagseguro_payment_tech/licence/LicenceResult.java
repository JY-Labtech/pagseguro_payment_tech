package br.com.jylabtech.pagseguro_payment_tech.licence;


public class LicenceResult {
    private String method = "licence";
    private String errorMessage;
    // Começa como erro: só vira sucesso quando a licença foi validada de fato.
    private int result = LicenceConfig.RESULT_ERROR;
    private String message;

    private CheckLicenceEntity licenceEntity;

    public static LicenceResult success(CheckLicenceEntity entity) {
        LicenceResult licenceResult = new LicenceResult();
        licenceResult.setResult(LicenceConfig.RESULT_SUCCESS);
        licenceResult.setLicenceEntity(entity);
        return licenceResult;
    }

    public static LicenceResult error(String errorMessage) {
        LicenceResult licenceResult = new LicenceResult();
        licenceResult.setResult(LicenceConfig.RESULT_ERROR);
        licenceResult.setErrorMessage(errorMessage);
        return licenceResult;
    }

    public boolean isSuccess() {
        return this.result == LicenceConfig.RESULT_SUCCESS;
    }

    public void setLicenceEntity(CheckLicenceEntity entity) {
        this.licenceEntity = entity;
    }

    public CheckLicenceEntity getLicenceEntity() {
        return this.licenceEntity;
    }

    public void setMethod(String value) {
        this.method = value;
    }

    public void setResult(int value) {
        this.result = value;
    }

    public void setErrorMessage(String value){ this.errorMessage = value;}

    public void setMessage(String value){this.message = value;}

    public String getMessage(){return this.message;}

    public String getMethod(){return this.method;}
    public String getErrorMessage(){return this.errorMessage;}
    public int getResult(){return this.result;}
}
