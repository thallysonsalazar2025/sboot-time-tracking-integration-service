# sboot-time-tracking-integration-service

Microserviço reativo e stateless para integração de eventos de ponto com o provedor **Secullum**.

## Objetivo

- Consultar eventos de ponto (faltas, atrasos, horas extras) a partir de integração externa.
- Normalizar os dados no modelo interno `TimeEvent`.
- Entregar os dados para um orquestrador sem persistir eventos sensíveis.

## Arquitetura

Camadas implementadas:

- `controller`
- `service`
- `adapter`
- `mapper`
- `client`
- `config`
- `repository` (somente `integration_config`)
- `domain`
- `dto`
- `exception`

## Fluxo de integração

1. `GET /api/v1/integrations/events` recebe `companyId` e `period`.
2. `TimeTrackingIntegrationService` busca `integration_config` ativa no H2.
3. `ProviderResolver` seleciona o adapter (`SecullumAdapter`).
4. `SecullumAdapter` chama `SecullumClient` com `WebClient` e aplica resiliência (retry, circuit breaker e timeout).
5. `SecullumMapper` converte `SecullumResponse` para `TimeEvent`.
6. Controller retorna `Flux<TimeEvent>` para o consumidor.

## SecullumAdapter

O adapter aplica os seguintes mecanismos para aumentar tolerância a falhas externas:

- **Retry**: tentativas automáticas em erro transitório.
- **Circuit Breaker**: evita pressão quando o provedor está instável.
- **Timeout**: evita bloqueio prolongado de pipeline distribuído.

## Persistência

- Permitida apenas para `integration_config` (H2 em memória).
- Eventos de ponto **não são persistidos**.

## Exemplo de requisição

```http
GET /api/v1/integrations/events?companyId=11111111-1111-1111-1111-111111111111&period=2026-03
X-Correlation-Id: 2d5457e2-3977-45d6-9512-1a0ab649fca1
```

## Exemplo de resposta

```json
[
  {
    "type": "OVERTIME",
    "date": "2026-03-15",
    "quantity": 2.50,
    "amount": 125.00
  }
]
```

## OpenAPI

Contrato disponível em `src/main/resources/contract.yaml`.
