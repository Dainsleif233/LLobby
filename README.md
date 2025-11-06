# LLobby

*这是一个轻量化的大厅插件，主要功能有传送点和子服玩家显示*

## 依赖

[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)  
[PlaceholderAPI](https://modrinth.com/plugin/placeholderapi)（可选）

## 代理

本插件的显示子服玩家功能依赖BungeeCord消息通道，请确保代理已启用

## 权限

llobby.admin：管理权限节点，默认op

## 命令

### /llobby

```
传送
/llobby [world] [location]
```
`world`：传送点所在世界，留空完全随机  
`location`：传送点名称，留空则在世界内随机

加入世界和重生时会自动传送

### /llobbyadmin

```
添加传送点
/llobbyadmin add <name> [nick]
删除传送点
/llobbyadmin remove <name>
重载配置
/llobbyadmin reload
```
`name`：传送点名称  
`nick`：传送点昵称，用于显示title

## 配置

```yaml
# 传送点配置
worlds:
  - name: world # 世界名
    nick: "主世界" # 可选，用于显示title
    locations:
      - name: example1_1 # 传送点名
        nick: "主城" # 可选，用于显示title
        position: [0, 64, 0 , 0, 0] # x y z yaw pitch
      - name: example1_2
        nick: "末地门"
        position: [100, 64, 100 , 0, 0]
  - name: world_nether
    nick: "下界"
    locations:
      - name: example2_1
        position: [0, 64, 0 , 0, 0]
# 子服玩家显示配置
servers:
  - server: lobby # 服务器名
    prefix: "§e[大厅] " # 显示的玩家前缀
yggdrasil-api: https://littleskin.cn/api/yggdrasil # 获取玩家头像的api，留空使用Mojang
```
`nick` 和 `prefix` 可用 `§` 指示颜色  
设置 `worlds: []` 以关闭传送功能  
不在 `servers` 中的服务器将不会显示玩家

## PlaceholderAPI变量

`llobby_prefix`：玩家前缀  
`llobby_world`：玩家所在世界昵称
