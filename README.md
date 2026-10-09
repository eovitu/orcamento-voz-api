# API de orçamento com reconhecimento de fala

Projeto educacional para o desafio DIO **Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot**. Implementação própria com Java 21, Spring Boot 3.5.16 e Spring AI 1.1.8.

Recebe áudio, transcreve com o modelo de transcrição, interpreta a pergunta usando `ChatClient`, consulta o orçamento por tool calling e sintetiza a resposta em voz. Também oferece endpoints independentes para transcrição, fala e gerenciamento de lançamentos fictícios.

## Executar sem chave ou cobrança

Pré-requisito: JDK 21 ou superior e acesso à internet para baixar dependências na primeira execução. O Maven Wrapper já está incluído.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Linux/macOS: `sh mvnw clean verify` e `sh mvnw spring-boot:run`.

A API fica em `http://127.0.0.1:8080`. Por padrão a IA está **desativada**: os endpoints de orçamento funcionam e os endpoints de IA retornam HTTP 503. Não há transcrição fictícia em produção. Os testes usam mocks e HTTP simulado e não precisam de chave.

O H2 em arquivo persiste os lançamentos em `data/`. Reiniciar a aplicação preserva os dados. `ddl-auto=update` foi escolhido para a demonstração; ambientes reais precisam de migrações versionadas.

## Experimentar com dados fictícios

```powershell
$entrada = '{"tipo":"RECEITA","valor":100.10,"descricao":"Receita ficticia","data":"2026-10-08"}'
Invoke-RestMethod http://127.0.0.1:8080/api/transacoes -Method Post -ContentType 'application/json' -Body $entrada
$entrada = '{"tipo":"DESPESA","valor":0.20,"descricao":"Despesa ficticia","data":"2026-10-08"}'
Invoke-RestMethod http://127.0.0.1:8080/api/transacoes -Method Post -ContentType 'application/json' -Body $entrada
Invoke-RestMethod http://127.0.0.1:8080/api/orcamento
```

Resultado: receitas `100.10`, despesas `0.20`, saldo `99.90`. Valores devem ser positivos, com no máximo duas casas decimais e 14 dígitos inteiros. A descrição é obrigatória, até 120 caracteres. A data é obrigatória. Um orçamento pode ter saldo negativo; não representa uma conta bancária ou limite de crédito.

## Ativar a integração real

Opcional: é necessário ter uma chave válida e acesso aos modelos no provedor. Ao ativar, áudio, perguntas e resultados das tools são enviados à OpenAI e podem gerar cobrança. Use somente dados fictícios neste projeto. Não coloque sua chave em arquivos, commits ou descrição da entrega.

No PowerShell, usando uma variável de ambiente já configurada fora do repositório:

```powershell
$env:AI_ENABLED = 'true'
# OPENAI_API_KEY deve existir no ambiente; não há chave no repositório.
.\mvnw.cmd spring-boot:run
```

Configurações opcionais: `CHAT_MODEL` (padrão `gpt-4o-mini`), `TRANSCRIPTION_MODEL` (`whisper-1`) e `SPEECH_MODEL` (`tts-1`). A disponibilidade e os preços dependem da sua conta no provedor. IA habilitada sem chave impede a inicialização, evitando configuração silenciosamente inválida.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/assistente/texto -Method Post -ContentType 'application/json' -Body '{"texto":"Qual meu saldo?"}'
curl.exe -X POST http://127.0.0.1:8080/api/assistente/audio -F 'arquivo=@voz.wav'
curl.exe -X POST http://127.0.0.1:8080/api/transcricoes -F 'arquivo=@voz.wav'
```

O assistente de áudio responde JSON com `transcricao`, `resposta`, `audioBase64` e `formato`. Para reproduzir o áudio, decodifique `audioBase64` como MP3. A voz é gerada por IA, não por uma pessoa. O endpoint `/api/fala` recebe `{"texto":"Olá"}` e devolve bytes `audio/mpeg`.

## Endpoints

| Método | Caminho | Entrada / saída |
| --- | --- | --- |
| POST | `/api/transacoes` | JSON com tipo `RECEITA`/`DESPESA`, valor, descrição e data; retorna 201 com lançamento |
| GET | `/api/transacoes` | Lista de lançamentos por ID |
| GET | `/api/orcamento` | Receitas, despesas e saldo calculados com `BigDecimal` |
| POST | `/api/assistente/texto` | JSON `texto`; resposta do ChatClient com tool de orçamento |
| POST | `/api/assistente/audio` | Multipart `arquivo`; transcrição, resposta e áudio Base64 |
| POST | `/api/transcricoes` | Multipart `arquivo`; JSON `texto` |
| POST | `/api/fala` | JSON `texto`; áudio MP3 |

Texto: obrigatório, até 2000 caracteres. Áudio: entre 1 byte e 10 MB; extensões mp3, wav, m4a, webm, mp4, mpeg e mpga. O nome é substituído por `audio.extensão`; o arquivo não é salvo em disco. A validação local verifica tamanho e extensão, não a autenticidade do conteúdo: a decodificação fica com o provedor. Entrada inválida retorna 400, upload acima do limite multipart retorna 413, IA desativada ou falha do provedor retorna 503, falha interna retorna 500. Erros não expõem corpos do provedor ou stack traces.

## Arquitetura

```text
ApiController -> AssistenteService -> Inteligencia
                                      |
                              SpringAiInteligencia
                          transcrição / ChatClient / fala
                                      |
                         @Tool consultarOrcamento
                                      |
                            OrcamentoService -> JPA -> H2
```

`OrcamentoTools` consulta dados reais do banco local. É uma função somente de leitura: o modelo não cria, exclui ou altera lançamentos. A criação ocorre por REST explícito. Não há movimentação financeira real. A integração usa as classes do Spring AI; não é um parser de palavras-chave simulando IA.

## Evidências de verificação

- **17 testes**: REST, validações, persistência JPA em H2, precisão monetária, erros, orquestração e integração do adaptador.
- `SpringAiIntegracaoTest` executa as classes reais `OpenAiChatModel`, `OpenAiAudioTranscriptionModel` e `OpenAiAudioSpeechModel`, com HTTP simulado. O modelo simulado solicita a tool; o Spring AI executa a função, recebe o saldo e devolve a resposta para síntese.
- Verificação manual do JAR: criação retornou um lançamento, consulta retornou `50.25` e o saldo permaneceu `50.25` após reiniciar usando H2 em arquivo. IA desativada retornou 503.
- **Não verificado:** reconhecimento de áudio real, qualidade das respostas, voz real e integração remota com uma chave do provedor. Os bytes de áudio dos testes são artificiais e não devem ser reproduzidos.

## Limites e evolução

Este projeto é local, monousuário e educacional. Não tem autenticação, isolamento por usuário, rate limiting, paginação ou idempotência para POST de lançamentos; repetir um POST cria outro lançamento. Não exponha o servidor em uma rede pública nem use dados pessoais. Segredos são lidos do ambiente; corpos de áudio e texto não são registrados pela aplicação. Antes de uso real: autenticação, autorização, isolamento, limites de custo/tempo, migrações, paginação, observabilidade sem dados sensíveis e testes de contrato com o provedor. O H2 e o armazenamento local substituem a necessidade de Docker para esta demonstração.

Referências: [Spring AI: ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html), [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html), [Spring Boot](https://docs.spring.io/spring-boot/).

Autor: [eovitu](https://github.com/eovitu).
