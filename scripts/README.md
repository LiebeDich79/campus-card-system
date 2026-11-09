# 复现脚本

两个脚本只依赖 Python 标准库，用来对运行中的后端发真实 HTTP 请求，
结果就是 `docs/接口实测记录.md` 的来源。

```bash
# 先按 README 启动后端（默认 8080）；若换端口：
export CAMPUS_BASE=http://localhost:18080

python scripts/run_api_tests.py           # 32 条功能/边界/异常用例
python scripts/run_concurrency_tests.py   # 100 线程并发 + 200 请求混合负载
```

并发脚本会真的扣款，请**在临时库上跑**，并在跑之前把
`campus.max-qps-per-terminal` 临时调高（例如 2000），否则默认的 20 次/秒限流会干扰结果。
脚本输出 JSON 到当前目录，方便自己核对。
