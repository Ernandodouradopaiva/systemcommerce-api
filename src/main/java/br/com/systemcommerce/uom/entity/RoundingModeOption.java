package br.com.systemcommerce.uom.entity;

import java.math.RoundingMode;

/** Espelha exatamente os nomes de {@link java.math.RoundingMode} usados na conversão de unidades. */
public enum RoundingModeOption {
    HALF_UP,
    HALF_DOWN,
    UP,
    DOWN,
    CEILING,
    FLOOR;

    public RoundingMode toJavaRoundingMode() {
        return RoundingMode.valueOf(name());
    }
}
