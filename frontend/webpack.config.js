const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const CompressionPlugin = require("compression-webpack-plugin");

module.exports = function (options = {}) {
  // Settings
  // --env NODE_ENV=production --env SOURCE_MAP=source-map ...
  const NODE_ENV = options.NODE_ENV || "development"; // "production"
  const SOURCE_MAP = options.SOURCE_MAP || "eval-source-map"; // "source-map"

  const SRC_DIR = path.resolve(__dirname, "src");

  console.error(`
Build started with following configuration:
===========================================
→ NODE_ENV: ${NODE_ENV}
→ SOURCE_MAP: ${SOURCE_MAP}
`);

  return {
    entry: {
      app: [
        path.resolve(SRC_DIR, "index.tsx")
      ]
    },
    output: {
      path: path.resolve(__dirname, "dist"),
      filename: "[name].js?[contenthash]",
      chunkFilename: "[name].bundle.js?[chunkhash]",
      publicPath: "/"
    },
    optimization: {
      splitChunks: {
        chunks: "all"
      }
    },
    resolve: {
      extensions: [".ts", ".tsx", ".js"]
    },
    bail: false,
    devtool: SOURCE_MAP,
    module: {
      rules: [
        {
          test: /\.tsx?$/,
          use: [{
            loader: "ts-loader"
          }]
        },
        {
          test: /\.s[ac]ss$/,
          use: [
            MiniCssExtractPlugin.loader,
            {
              loader: "css-loader",
              options: {
                modules: {
                  localIdentName: "[path][name]__[local]--[hash:base64:5]"
                }
              }
            },
            {
              loader: "sass-loader"
            }
          ]
        },
        {
          test: /\.(png|jpg|gif|svg)$/,
          type: "asset",
          parser: {
            dataUrlCondition: {
              maxSize: 32768
            }
          }
        }
      ]
    },
    plugins: [
      new webpack.DefinePlugin({
        "process.env": {
          "NODE_ENV": JSON.stringify(NODE_ENV)
        }
      }),
      new MiniCssExtractPlugin(),
      new HtmlWebpackPlugin({
        filename: "index.html",
        template: "src/index.html",
      }),
      new CompressionPlugin({
        algorithm: "gzip",
      }),
      new CompressionPlugin({
        algorithm: "brotliCompress",
      }),
    ],
    stats: {
      errorDetails: true,
    },
  }
};
