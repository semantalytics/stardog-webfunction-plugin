[![Build Status](https://travis-ci.org/semantalytics/stardog-wasm.svg?branch=master)](https://travis-ci.org/semantalytics/stardog-wasm)

# Stardog WebFunctions

A [Stardog](http://stardog.com) plugin for executing web assembly functions

```prefix wf: <http://semantalytics.com/2021/03/ns/stardog/webfunction/>
prefix f: <ipfs://QmVx8jryTscgnbJoh8iuUYUiiGBeu4tr1i1A3PmCqcE5Vk/>

select ?result where { bind(wf:call(f:toUpper, \"stardog\") AS ?result) }";
```

Stardog WebFunctions supports loading functions from IPFS to reduce external dependencies and allow functions to e called offline
