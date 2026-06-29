name: Unzip

on:
  push:
    branches: [ main ]

jobs:
  unzip:
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4

      # Nhớ đổi "namefilr.zip" thành tên file zip thật của bạn nếu nó tên khác nhé
      - name: Extract
        run: unzip -o acore.zip 

      - name: Commit extracted files
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git add .
          git commit -m "Extract zip [skip ci]" || echo "Nothing to commit"
          git push
          
