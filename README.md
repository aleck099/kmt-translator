# A plugin for [mamoe/mirai-console](https://github.com/mamoe/mirai-console)

Automatically translate members' messages in QQ group

Support ja <=> zh



Version: 1.1

Mirai version: 2.9.0-M1

Backend: Baidu Fanyi ([https://fanyi-api.baidu.com/](https://fanyi-api.baidu.com/))



## Build:

`./gradlew buildPlugin`

And find the jar in `build/mirai/`



## Usage:

Copy the jar into `plugins/`

Create configuration file in `config/kmt-translator/kmt-translator.yml`:

```yaml
appid: <your-appid>
appkey: <your-key>
logfile: log.txt
imagedir: images
whitelist: 
  - <qq accounts>
```

Remember to create data/kmt-translator/images for it to store images in messages


Whitelisted members' every word will be translated

Normal users put "/tr" **at their first line** to invoke translation

e.g.

```
/tr
你好
```

Images,  faces, quotes won't be translated



You can specify the target language you want

```
/tr c
テスト

/tr j
测试
```

/tr c = Translate **to** Chinese





*As long as you love tentacles, we are comrades.*

