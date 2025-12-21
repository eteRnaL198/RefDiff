```bash
./gradlew runInvestigation --args='-l java -s 07194c1d550787efbf629b786aeee19d0b6123fc'
```

 ```bash
# Dockerイメージをビルドします (初回のみ)
docker build -t refdiff-investigation .
 
# コンテナを実行します
docker run --rm -it \
  -v /Users/ikuya/Documents/TokyoTech/Research/RefDiff/investigation/repo:/home/gradle/project/investigation/repo \
  -v /Users/ikuya/Documents/TokyoTech/Research/RefDiff/investigation/result:/home/gradle/project/investigation/result \
  refdiff-investigation  
```