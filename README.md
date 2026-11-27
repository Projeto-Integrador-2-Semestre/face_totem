# FaceTotem

App Android nativo (Java) que funciona como totem de catraca: a pessoa posiciona o rosto
na frente do tablet, o app envia frames da câmera para a `face_api` e mostra
**LIBERADO** ou **NEGADO**.

## Fluxo das telas

1. **Inicial** – "fazer reconhecimento" com o botão de câmera. No topo mostra se a API está online.
2. **Câmera** – preview da câmera frontal com a moldura. A cada ~0,8 s um frame vira JPEG e vai
   para `POST /faces/recognize`.
   - `nenhum_rosto` / `rosto_cortado`: continua tentando (a pessoa está se posicionando)
   - `autorizado`, `nao_reconhecido` ou `rosto_nao_vivo`: abre a tela de resultado
   - 15 s sem resultado decisivo: volta para a inicial
3. **Resultado** – tela verde com o nome ou vermelha, com aviso sonoro. Após 5 s volta para a inicial.

## Estrutura

```
app/src/main/java/br/com/pi2/facetotem/
├── TotemActivity.java        base: tela sempre ligada, barras do sistema escondidas
├── MainActivity.java         tela inicial + configuração escondida (toque longo no título)
├── CameraActivity.java       CameraX (Preview + ImageAnalysis) → JPEG → API
├── ResultadoActivity.java    LIBERADO / NEGADO por 5 s
├── Config.java               SharedPreferences: URL da API e modo launcher
└── api/
    ├── ApiClient.java        Retrofit + OkHttp (timeouts curtos, log no Logcat)
    ├── FaceApi.java          GET /health, POST /faces/recognize
    ├── HealthResponse.java
    └── ReconhecimentoResponse.java
```

## API usada pelo app

A URL padrão é a API publicada no Render: `https://faceapi-pd4q.onrender.com/`. Ela fica em
`Config.URL_PADRAO` e pode ser trocada sem recompilar na configuração escondida (toque longo no
título "fazer reconhecimento"). Apagar o campo e salvar volta para a URL padrão; faça isso num
aparelho que ainda tenha uma URL antiga salva.

No plano Free o Render desliga a API depois de 15 min sem uso. A primeira requisição depois disso
leva perto de 1 min e estoura os timeouts do app (`ApiClient`): espere a API acordar
(`/health` com `"pronta": true`) e tente de novo.

## Rodando em desenvolvimento (API no PC)

1. Suba a API no PC, na pasta `face_api`:
   ```
   python -m uvicorn main:app --host 0.0.0.0 --port 8000
   ```
2. Abra o projeto no Android Studio e rode no emulador. Na configuração escondida, informe
   `http://10.0.2.2:8000/`, que é o localhost do PC visto de dentro do emulador.
3. Na primeira abertura o Android pede duas permissões: **rede local** (Android 16+) e **câmera**.
   Aceite as duas.
4. Para testar com um rosto real no emulador: *Device Manager → editar o AVD → Show Advanced
   Settings → Camera → Front: Webcam0* e reinicie o emulador.

### Tablet físico

- PC e tablet na mesma rede Wi-Fi; a API precisa rodar com `--host 0.0.0.0`.
- No app, toque longo no título "fazer reconhecimento" e informe a URL com o IP do PC
  (ex.: `http://192.168.0.10:8000/`). Dê um IP fixo ao PC.
- Alternativa sem Wi-Fi, com o tablet no USB: `adb reverse tcp:8000 tcp:8000` e URL
  `http://127.0.0.1:8000/`.

## Modo totem

- Tela sempre ligada e barras do sistema escondidas em todas as telas (`TotemActivity`).
- **Iniciar junto com o aparelho**: na configuração escondida, marque "modo launcher". Depois
  aperte Home e escolha FaceTotem como launcher padrão ("Sempre"). O app passa a abrir sozinho
  no boot. Para reverter, desmarque a opção.
- Para impedir totalmente a saída do app (lock task / device owner) é preciso provisionar o
  tablet como dispositivo gerenciado; não está implementado.

## Liberação manual pelo painel web

O botão **Liberar Catraca** do painel web (front-end) pede a liberação de uma catraca na API Java de gestão
(`ApiControleDeAcessoPI2`). Para o totem receber esse pedido, informe na configuração escondida:

- **URL da API de gestão** – ex.: `http://192.168.0.10:8080/` (ou `http://10.0.2.2:8080/` no emulador);
- **ID da catraca deste totem** – o `id` da catraca em `GET /catracas` (ex.: `1`).

O ID da catraca também vai junto de cada foto enviada para a face_api (`catraca_id`), e o painel de logs passa a
mostrar em qual catraca a pessoa passou. Sem ID configurado, os logs aparecem como "Totem de Reconhecimento Facial".

Com os dois preenchidos, as telas inicial e de câmera consultam `POST /catracas/{id}/liberacao/confirmar` a
cada 1,5 s (`LiberacaoRemota`). Quando há uma liberação, abre a tela verde **ACESSO LIBERADO** com o motivo e o
nome do operador. Deixe os campos em branco para desligar.

## Pessoa bloqueada

Quando o operador bloqueia uma pessoa no painel, a face_api continua reconhecendo o rosto, mas responde
`motivo: "pessoa_bloqueada"`. O totem mostra a tela vermelha **BLOQUEADO** com o nome da pessoa e
"Seu acesso está bloqueado. Procure a recepção.", e nenhuma passagem é registrada.

## Pendências de hardware

- **Acionamento físico da catraca**: o app só mostra o resultado. O ponto para integrar está
  marcado em `ResultadoActivity` (comentário). Opções: a API aciona um relé/ESP32 na rede, ou o
  tablet fala com o controlador por Wi-Fi ou USB.
- **Qualidade da câmera frontal**: afeta o anti-spoofing. Se pessoas reais forem recusadas com
  `rosto_nao_vivo`, ajuste `LIVENESS_THRESHOLD` na API (0.6–0.7).

## Observações técnicas

- HTTP sem TLS: o build de **release** só aceita HTTPS (as APIs no Render), com exceção de `127.0.0.1`,
  `localhost` e `10.0.2.2` (`src/main/res/xml/network_security_config.xml`). O build de **debug** (Run do
  Android Studio) libera HTTP para qualquer endereço (`src/debug/res/xml/network_security_config.xml`), para
  testar com a API no PC pelo IP da rede. Para usar um IP local num build de release, acrescente-o no
  `<domain-config>` do arquivo de `main`.
- `gradle/gradle-daemon-jvm.properties` fixa o JDK do build no JetBrains Runtime embutido no
  Android Studio (`toolchainVendor=JETBRAINS`), evitando que um JRE sem `jlink` seja escolhido.
