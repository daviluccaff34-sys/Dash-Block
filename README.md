# Dash Block

Primeira versão jogável de um jogo de plataforma 2D para Android.

## Versão 0.1.0

- Jogo em modo paisagem.
- Toque na tela para começar e pular.
- Desvie dos obstáculos.
- Pontuação e recorde durante a sessão.
- Colisão e reinício por toque.
- Trilha sonora `CrushTekk.mp3` em loop.
- Projeto Android nativo leve, sem engine externa.

## Gerar o APK

O GitHub Actions compila automaticamente quando há alterações na branch `main`.

1. Abra a aba **Actions** do repositório.
2. Selecione **Build Dash Block APK**.
3. Abra uma execução concluída.
4. Em **Artifacts**, baixe `Dash-Block-Android-APK`.
5. Extraia o ZIP para encontrar `app-debug.apk`.

Também é possível iniciar uma compilação manual pelo botão **Run workflow**.

## Controles

- Toque: começar.
- Toque no ar/chão: pular (o pulo só acontece quando o personagem está no chão).
- Colidiu: toque para reiniciar.

A pasta `assets/music/` guarda a música usada pelo jogo.
