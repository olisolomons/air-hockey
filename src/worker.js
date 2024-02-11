import { handler } from '../out/cljs-cf';

export default {
	async fetch(request, env, ctx) {
		return handler(request, env, ctx);
	},
};
