build:
	npx shadow-cljs release :app

repl:
	clj -M:dev:nrepl -m nrepl.cmdline --middleware "[shadow.cljs.devtools.server.nrepl/middleware]"
