build:
	npx shadow-cljs release :cf

register:
	env $$(cat .env | xargs) clj -X register-commands/register
