import * as React from "react";
import * as ReactDOM from "react-dom";
import styles from "./index.scss";

ReactDOM.render(
  <div>
    <div className={styles["hello_world"]}>Hello, World!</div>
  </div>,
  document.getElementById("root")
);

