import { describe, expect, it } from "vitest";
import { money, statusLabel } from "./format";

describe("formatadores do frontend", () => {
  it("formata valores BRL recebidos do backend", () => {
    expect(money({ amount: "1234.50", currency: "BRL" })).toContain("1.234,50");
  });

  it("não trata status técnico como regra comercial nova", () => {
    expect(statusLabel("PENDING_PAYMENT")).toBe("Pagamento pendente");
    expect(statusLabel("UNKNOWN_STATE")).toBe("Unknown state");
  });
});
