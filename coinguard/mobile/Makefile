clean:
	# see issue: https://github.com/pmadruga/react-native-clean-project/issues/45
  # the following command is not available to npx.
	# @npx react-native-clean-project-auto
  # see https://github.com/pmadruga/react-native-clean-project#content
	@npx react-native-clean-project --keep-brew --keep-pods

install-deps:
	@npm install
	@npx pod-install ios

lint-kondo:
	@echo "lint-kondo:"
	@clojure -M:dev -m clj-kondo.main --config ./.clj-kondo/config.edn --lint src/

lint-fmt:
	@echo "lint-fmt:"
	@TARGETS=$$(git diff --diff-filter=d --cached --name-only --relative src && echo src) && \
	clojure -M:dev -m cljfmt.main check --indents ./.cljfmt/indentation.edn $$TARGETS

lint-fmt-fix:
	@clojure -M:dev -m cljfmt.main fix src/ --indents ./.cljfmt/indentation.edn

lint-carve:
	@echo "lint-carve:"
	@clojure -M:carve 

lint: lint-kondo lint-carve lint-fmt
	@true

# see https://shadow-cljs.github.io/docs/UsersGuide.html#target-node-test
# for how test works.
test:
	@clojure -M:shadow compile mocks && \
	clojure -M:shadow compile test && \
	node --require ./test-resources/override.js target/test/test.js

run-shadow:
	@clojure -M:shadow watch app

run-repl:
	@clojure -M:shadow cljs-repl app

run-metro:
	@npx react-native start

run-android:
	@npx react-native run-android 

run-ios:
	@npx react-native run-ios
