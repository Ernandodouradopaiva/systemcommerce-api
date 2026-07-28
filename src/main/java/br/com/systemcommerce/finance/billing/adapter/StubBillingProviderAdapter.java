package br.com.systemcommerce.finance.billing.adapter;

import br.com.systemcommerce.finance.billing.entity.BillingDocument;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stub local — gera identificadores sem chamar banco real. */
@Component
public class StubBillingProviderAdapter implements BillingProviderAdapter {

    public static final String CODE = "STUB";

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public BankSlipRegistration registerBankSlip(BillingDocument document) {
        String nosso = "STUB" + System.currentTimeMillis() % 1_000_000_000L;
        String digitable = "23793.38128 60000.000003 00000.000400 " + nosso.substring(Math.max(0, nosso.length() - 1))
                + " " + document.getAmount().movePointRight(2).toPlainString();
        return new BankSlipRegistration(
                "stub-boleto-" + UUID.randomUUID(),
                digitable.length() > 80 ? digitable.substring(0, 80) : digitable,
                "2379" + nosso,
                nosso,
                "237",
                "09",
                null);
    }

    @Override
    public PixRegistration registerPix(BillingDocument document, Instant expiresAt) {
        String txid = ("stub" + UUID.randomUUID().toString().replace("-", "")).substring(0, 32);
        String copyPaste = "00020126580014BR.GOV.BCB.PIX0136" + txid + "520400005303986540"
                + document.getAmount().toPlainString() + "5802BR5925SYSTEMCOMMERCE STUB6009SAO PAULO62070503***6304ABCD";
        return new PixRegistration(
                "stub-pix-" + UUID.randomUUID(),
                txid,
                copyPaste,
                null,
                copyPaste,
                expiresAt);
    }

    @Override
    public void cancel(BillingDocument document) {
        // no-op no stub
    }
}
