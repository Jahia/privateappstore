// Minimal ESLint config focused on real bugs, not style.
// Style enforcement is deliberately out of scope — webpack/babel handle parsing,
// and there's no Prettier in this project. Add stylistic rules only if the team
// adopts them broadly across the Jahia module estate.
module.exports = {
    root: true,
    parserOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        ecmaFeatures: {jsx: true}
    },
    env: {
        browser: true,
        es2022: true,
        node: true
    },
    settings: {
        react: {version: '18.3'}
    },
    plugins: ['react', 'react-hooks'],
    extends: [
        'eslint:recommended',
        'plugin:react/recommended',
        'plugin:react-hooks/recommended'
    ],
    rules: {
        // React 17+ JSX transform no longer needs React in scope per file.
        'react/react-in-jsx-scope': 'off',
        // Props are validated by Java types on the server side and PropTypes is
        // not exhaustively wired through Moonstone here.
        'react/prop-types': 'off',
        // Allow console.* — the existing code uses console.debug for activation logs.
        'no-console': 'off',
        // Anonymous function expressions in registry callbacks are intentional.
        'func-names': 'off',
        // Prefer named warnings to silently passing on unused vars; ignore underscore-prefixed.
        'no-unused-vars': ['warn', {argsIgnorePattern: '^_', varsIgnorePattern: '^_'}]
    },
    overrides: [
        {
            files: ['*.cjs'],
            env: {node: true, browser: false}
        }
    ]
};
