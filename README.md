# Kafkaを使ってメッセージ送受信する
## 実行前にKafkaを起動しておく
``` bash
> docker-compose up -d

```
## アプリケーションを起動する
``` bash
> sbt run
```

## Kafkaで受信したメッセージを出力するようしておく
```bash
> docker exec -it cli /bin/bash
root@cli:/# kafka-console-consumer --bootstrap-server broker:29092 --topic copy-topic --group copy-group --from-beginning
```

## Kafkaにメッセージを送信する
``` bash
> docker exec -it broker /bin/bash
root@broker:/# kafka-console-producer --broker-list broker:29092 --topic practice-topic
>{"title":"sample", "text":"example"}
>invalid message
>{"title":"hoge", "text":"fuga"}
```

## 送信したメッセージが加工されて送信されてくることが確認できる
```bash
> docker exec -it cli /bin/bash
root@cli:/# kafka-console-consumer --bootstrap-server broker:29092 --topic copy-topic --group copy-group --from-beginning
copy message - title:sample, text:example
copy message - title:hoge, text:fuga
```
