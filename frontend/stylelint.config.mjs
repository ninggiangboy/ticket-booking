// Colours only through tokens (DOC-39 §1, DR-134): no literal colours outside styles/tokens.css.
export default {
  plugins: ["stylelint-declaration-strict-value"],
  rules: {
    "color-no-hex": true,
    "scale-unlimited/declaration-strict-value": [
      [
        "/color$/",
        "background",
        "background-color",
        "border-color",
        "fill",
        "stroke",
        "outline-color",
      ],
      {
        ignoreValues: ["currentColor", "transparent", "inherit", "none", "initial", "unset"],
        ignoreFunctions: false,
        disableFix: true,
      },
    ],
  },
  overrides: [
    {
      files: ["src/styles/tokens.css"],
      rules: { "color-no-hex": null, "scale-unlimited/declaration-strict-value": null },
    },
  ],
};
