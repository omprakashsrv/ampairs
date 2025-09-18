import {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger
} from "./chunk-VOPX6JSL.js";
import {
  MatOptgroup,
  MatOption
} from "./chunk-E6AJGTAP.js";
import "./chunk-XA3EVXH3.js";
import "./chunk-FRRWDKXC.js";
import "./chunk-XWUVEPIS.js";
import {
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatPrefix,
  MatSuffix
} from "./chunk-6IQKCZZP.js";
import "./chunk-SBUJITM6.js";
import "./chunk-AZ344G2Z.js";
import "./chunk-EUMELCXQ.js";
import "./chunk-TD5Q3PZX.js";
import "./chunk-PJKUSW3X.js";
import "./chunk-LAJUCN66.js";
import "./chunk-D4FSAXW7.js";
import "./chunk-PZ6BVART.js";
import "./chunk-46HAYV32.js";
import "./chunk-7VQLIE7N.js";
import "./chunk-O6GUYFJF.js";
import "./chunk-JUY6F3JW.js";
import "./chunk-HVMEPI33.js";
import "./chunk-VENV3F3G.js";
import "./chunk-H2OR3B6R.js";
import "./chunk-QNXOKN2T.js";
import "./chunk-TXJ6TSDA.js";
import "./chunk-ZYT3AMJR.js";
import "./chunk-7UJZXIJQ.js";
import "./chunk-HLEF2NZP.js";
import "./chunk-77GYRMY5.js";
import "./chunk-HYQ7UB74.js";
import "./chunk-C6LNL6PX.js";
import "./chunk-CSPLI7JI.js";
import "./chunk-HSQL6P5L.js";
import "./chunk-5BMBN4WP.js";
import "./chunk-HWYXSU2G.js";
import "./chunk-JRFR6BLO.js";
import "./chunk-MARUHEWW.js";
import "./chunk-WDMUDEB6.js";

// node_modules/@angular/material/fesm2022/select.mjs
var matSelectAnimations = {
  // Represents
  // trigger('transformPanel', [
  //   state(
  //     'void',
  //     style({
  //       opacity: 0,
  //       transform: 'scale(1, 0.8)',
  //     }),
  //   ),
  //   transition(
  //     'void => showing',
  //     animate(
  //       '120ms cubic-bezier(0, 0, 0.2, 1)',
  //       style({
  //         opacity: 1,
  //         transform: 'scale(1, 1)',
  //       }),
  //     ),
  //   ),
  //   transition('* => void', animate('100ms linear', style({opacity: 0}))),
  // ])
  /** This animation transforms the select's overlay panel on and off the page. */
  transformPanel: {
    type: 7,
    name: "transformPanel",
    definitions: [
      {
        type: 0,
        name: "void",
        styles: {
          type: 6,
          styles: { opacity: 0, transform: "scale(1, 0.8)" },
          offset: null
        }
      },
      {
        type: 1,
        expr: "void => showing",
        animation: {
          type: 4,
          styles: {
            type: 6,
            styles: { opacity: 1, transform: "scale(1, 1)" },
            offset: null
          },
          timings: "120ms cubic-bezier(0, 0, 0.2, 1)"
        },
        options: null
      },
      {
        type: 1,
        expr: "* => void",
        animation: {
          type: 4,
          styles: { type: 6, styles: { opacity: 0 }, offset: null },
          timings: "100ms linear"
        },
        options: null
      }
    ],
    options: {}
  }
};
export {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatOptgroup,
  MatOption,
  MatPrefix,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger,
  MatSuffix,
  matSelectAnimations
};
//# sourceMappingURL=@angular_material_select.js.map
