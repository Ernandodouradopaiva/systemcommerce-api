package br.com.systemcommerce.customer.validation;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;

public final class BrazilianDocumentUtils {

    private BrazilianDocumentUtils() {}

    public static String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String digits) {
        if (digits == null || digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int d1 = calcCpfDigit(digits, 10);
            int d2 = calcCpfDigit(digits, 11);
            return digits.charAt(9) - '0' == d1 && digits.charAt(10) - '0' == d2;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isValidCnpj(String digits) {
        if (digits == null || digits.length() != 14 || digits.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int d1 = calcCnpjDigit(digits, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
            int d2 = calcCnpjDigit(digits, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
            return digits.charAt(12) - '0' == d1 && digits.charAt(13) - '0' == d2;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static String normalizeAndValidate(Customer.CustomerType type, String rawDocument) {
        String digits = digitsOnly(rawDocument);
        if (digits == null || digits.isBlank()) {
            throw new BusinessRuleException("CPF/CNPJ é obrigatório");
        }

        if (type == Customer.CustomerType.PF) {
            if (digits.length() != 11) {
                throw new BusinessRuleException("Pessoa física exige CPF com 11 dígitos");
            }
            if (!isValidCpf(digits)) {
                throw new BusinessRuleException("CPF inválido");
            }
            return digits;
        }

        if (type == Customer.CustomerType.PJ) {
            if (digits.length() != 14) {
                throw new BusinessRuleException("Pessoa jurídica exige CNPJ com 14 dígitos");
            }
            if (!isValidCnpj(digits)) {
                throw new BusinessRuleException("CNPJ inválido");
            }
            return digits;
        }

        throw new BusinessRuleException("Tipo de pessoa inválido");
    }

    /**
     * Valida CPF/CNPJ sem acoplar ao enum de Customer — uso por Fornecedor e demais cadastros PF/PJ.
     */
    public static String normalizeAndValidatePfOrPj(boolean physicalPerson, String rawDocument) {
        return normalizeAndValidate(
                physicalPerson ? Customer.CustomerType.PF : Customer.CustomerType.PJ, rawDocument);
    }

    public static void assertValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String value = email.trim();
        if (!value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessRuleException("E-mail inválido");
        }
    }

    public static void assertUniqueDocument(boolean exists) {
        if (exists) {
            throw new ConflictException("CPF/CNPJ já está cadastrado");
        }
    }

    private static int calcCpfDigit(String digits, int weightStart) {
        int sum = 0;
        for (int i = 0; i < weightStart - 1; i++) {
            sum += (digits.charAt(i) - '0') * (weightStart - i);
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private static int calcCnpjDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }
}
