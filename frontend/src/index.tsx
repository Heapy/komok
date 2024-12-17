import * as React from "react";
import {StrictMode} from "react";
import * as ReactDOM from "react-dom/client";
import styles from "./index.scss";

const rootElement = document.getElementById("root") as HTMLElement;
const root = ReactDOM.createRoot(rootElement);

root.render(
      <StrictMode>
        <div>
          <div className={styles["hello_world"]}>Hello, World!</div>
        </div>
      </StrictMode>
);
