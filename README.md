# A plugin for [mamoe/mirai-console](https://github.com/mamoe/mirai-console)

Automatically translate members' messages in QQ group

Support ja <=> zh



Version: 1.4

Mirai version: 2.9.2

Backends:
- Baidu Fanyi ([https://fanyi-api.baidu.com/](https://fanyi-api.baidu.com/))
- Tencent Fanyi ([https://fanyi.qq.com/](https://fanyi.qq.com/))



## Build:

`./gradlew buildPlugin`

And find the jar in `build/mirai/`



## Usage:

Copy the jar into `plugins/`

Create configuration file in `config/<plugin name>/kmt-translator.yml`:

(`<plugin name>` may change when mirai-console updates, it is currently `net.accel.kmt.translator`)

```yaml
appid: <your-appid>
appkey: <your-key>
whitelist: 
  - <qq accounts>
group_whitelist:
  - <group ids>
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
/tr zh ja
テスト

/tr ja zh
测试
```



*As long as you love tentacles, we are comrades.*

