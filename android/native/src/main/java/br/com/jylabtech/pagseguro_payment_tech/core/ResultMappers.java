package br.com.jylabtech.pagseguro_payment_tech.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagUserDataResult;

/**
 * Conversões puras dos objetos do SDK PlugPag para o formato enviado ao Dart.
 * Sem Android/MethodChannel: testadas na JVM.
 */
public final class ResultMappers {
  private ResultMappers() {
  }

  /**
   * Parcelas do SDK para a lista de mapas lida por {@code PlugPagInstallment.fromMap} no Dart
   * (chaves {@code quantity}, {@code amount} e {@code total}, valores em centavos).
   */
  public static List<Map<String, Object>> installmentsToMaps(List<PlugPagInstallment> installments) {
    List<Map<String, Object>> resultList = new ArrayList<>();
    if (installments == null) {
      return resultList;
    }
    for (PlugPagInstallment installment : installments) {
      if (installment == null) {
        continue;
      }
      Map<String, Object> installmentMap = new HashMap<>();
      installmentMap.put("quantity", installment.getQuantity());
      installmentMap.put("amount", installment.getAmount());
      installmentMap.put("total", installment.getTotal());
      resultList.add(installmentMap);
    }
    return resultList;
  }

  /** Dados do estabelecimento do SDK para o JSON enviado em onFinishedResponse (method "userData"). */
  public static UserDataResult toUserDataResult(PlugPagUserDataResult userDataResult) {
    UserDataResult userResult = new UserDataResult();
    userResult.setMethod("userData");
    if (userDataResult == null) {
      return userResult;
    }
    userResult.setAddress(userDataResult.getAddress());
    userResult.setCity(userDataResult.getCity());
    userResult.setCnpjCpf(userDataResult.getCnpjCpf());
    userResult.setAddressComplement(userDataResult.getAddressComplement());
    userResult.setCompanyName(userDataResult.getCompanyName());
    userResult.setUserNickName(userDataResult.getUserNickName());
    userResult.setAddressState(userDataResult.getAddressState());
    userResult.setEmail(userDataResult.getEmail());
    return userResult;
  }
}
