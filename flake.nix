{
  description = "An airhockey web game for mobile";
  inputs.flake-utils.url = "github:numtide/flake-utils";
  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let pkgs = nixpkgs.legacyPackages.${system};
      in {
        defaultPackage = pkgs.hello;
        devShell = pkgs.mkShell {
          buildInputs = [pkgs.clojure pkgs.clojure-lsp pkgs.nodejs pkgs.jdk21 ];
        };
      }
    );
}
