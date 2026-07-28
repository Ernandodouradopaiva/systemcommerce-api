package br.com.systemcommerce.security.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void shouldAcceptStrongPassword() {
        assertThat(validator.isValid("Admin@123", null)).isTrue();
        assertThat(validator.isValid("Str0ng!Pass", null)).isTrue();
    }

    @Test
    void shouldRejectWeakPasswords() {
        assertThat(validator.isValid("alllowercase1!", null)).isFalse();
        assertThat(validator.isValid("ALLUPPERCASE1!", null)).isFalse();
        assertThat(validator.isValid("NoDigits!!Aa", null)).isFalse();
        assertThat(validator.isValid("NoSpecial12Aa", null)).isFalse();
        assertThat(validator.isValid("Ab1!", null)).isFalse();
        assertThat(validator.isValid("a".repeat(73) + "A1!", null)).isFalse();
    }
}
