package com.perfumestore.backend.address;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AddressDtos {

    private AddressDtos() {
    }

    public record AddressRequest(
            @NotBlank(message = "Informe o nome do destinatário.")
            @Size(min = 2, max = 120, message = "O nome do destinatário deve ter entre 2 e 120 caracteres.")
            String recipientName,
            @Size(max = 60, message = "A identificação do endereço deve ter até 60 caracteres.")
            String label,
            @NotBlank(message = "Informe o CEP.")
            @Pattern(regexp = "[0-9]{5}-?[0-9]{3}", message = "Informe um CEP válido.")
            String postalCode,
            @NotBlank(message = "Informe o logradouro.")
            @Size(min = 2, max = 180, message = "O logradouro deve ter entre 2 e 180 caracteres.")
            String street,
            @NotBlank(message = "Informe o número.")
            @Size(max = 30, message = "O número deve ter até 30 caracteres.")
            String number,
            @Size(max = 120, message = "O complemento deve ter até 120 caracteres.")
            String complement,
            @NotBlank(message = "Informe o bairro.")
            @Size(min = 2, max = 120, message = "O bairro deve ter entre 2 e 120 caracteres.")
            String neighborhood,
            @NotBlank(message = "Informe a cidade.")
            @Size(min = 2, max = 120, message = "A cidade deve ter entre 2 e 120 caracteres.")
            String city,
            @NotBlank(message = "Informe a UF.")
            @Pattern(regexp = "(?i)^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
                    message = "Informe uma UF brasileira válida.")
            String state,
            @Size(max = 180, message = "A referência deve ter até 180 caracteres.")
            String reference) {
    }

    public record AddressUpdateRequest(
            @NotBlank(message = "Informe o nome do destinatário.")
            @Size(min = 2, max = 120, message = "O nome do destinatário deve ter entre 2 e 120 caracteres.")
            String recipientName,
            @Size(max = 60, message = "A identificação do endereço deve ter até 60 caracteres.")
            String label,
            @NotBlank(message = "Informe o CEP.")
            @Pattern(regexp = "[0-9]{5}-?[0-9]{3}", message = "Informe um CEP válido.")
            String postalCode,
            @NotBlank(message = "Informe o logradouro.")
            @Size(min = 2, max = 180, message = "O logradouro deve ter entre 2 e 180 caracteres.")
            String street,
            @NotBlank(message = "Informe o número.")
            @Size(max = 30, message = "O número deve ter até 30 caracteres.")
            String number,
            @Size(max = 120, message = "O complemento deve ter até 120 caracteres.")
            String complement,
            @NotBlank(message = "Informe o bairro.")
            @Size(min = 2, max = 120, message = "O bairro deve ter entre 2 e 120 caracteres.")
            String neighborhood,
            @NotBlank(message = "Informe a cidade.")
            @Size(min = 2, max = 120, message = "A cidade deve ter entre 2 e 120 caracteres.")
            String city,
            @NotBlank(message = "Informe a UF.")
            @Pattern(regexp = "(?i)^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
                    message = "Informe uma UF brasileira válida.")
            String state,
            @Size(max = 180, message = "A referência deve ter até 180 caracteres.")
            String reference,
            @NotNull(message = "Informe a versão atual do endereço.") Long version) {
    }

    public record AddressResponse(UUID id, String recipientName, String label, String postalCode, String street,
            String number, String complement, String neighborhood, String city, String state, String reference,
            long version, Instant createdAt, Instant updatedAt) {
    }

    /** Dados normalizados para os serviços internos, sem expor o proprietário na API. */
    public record AddressData(UUID id, UUID userId, String postalCode, String city, String state) {
    }
}
