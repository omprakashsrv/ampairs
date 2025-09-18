import {
  FormsModule,
  NG_VALUE_ACCESSOR
} from "./chunk-EUMELCXQ.js";
import {
  isPlatformBrowser
} from "./chunk-C6LNL6PX.js";
import "./chunk-CSPLI7JI.js";
import {
  Component,
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Inject,
  Injectable,
  Input,
  NgModule,
  NgZone,
  Optional,
  Output,
  PLATFORM_ID,
  setClassMetadata,
  ɵɵProvidersFeature,
  ɵɵattribute,
  ɵɵdefineComponent,
  ɵɵdefineDirective,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵlistener
} from "./chunk-HSQL6P5L.js";
import {
  InjectionToken,
  forwardRef,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵinject
} from "./chunk-5BMBN4WP.js";
import "./chunk-HWYXSU2G.js";
import "./chunk-JRFR6BLO.js";
import {
  BehaviorSubject,
  Subject,
  filter,
  of
} from "./chunk-MARUHEWW.js";
import "./chunk-WDMUDEB6.js";

// node_modules/ng-recaptcha-2/fesm2022/ng-recaptcha-2.mjs
var RECAPTCHA_LANGUAGE = new InjectionToken("recaptcha-language");
var RECAPTCHA_BASE_URL = new InjectionToken("recaptcha-base-url");
var RECAPTCHA_NONCE = new InjectionToken("recaptcha-nonce-tag");
var RECAPTCHA_SETTINGS = new InjectionToken("recaptcha-settings");
var RECAPTCHA_V3_SITE_KEY = new InjectionToken("recaptcha-v3-site-key");
var RECAPTCHA_LOADER_OPTIONS = new InjectionToken("recaptcha-loader-options");
function loadScript(renderMode, onBeforeLoad, onLoaded, {
  url,
  lang,
  nonce
} = {}) {
  window.ng2recaptchaloaded = () => {
    onLoaded(grecaptcha);
  };
  const script = document.createElement("script");
  script.innerHTML = "";
  const {
    url: baseUrl,
    nonce: onBeforeLoadNonce
  } = onBeforeLoad(new URL(url || "https://www.google.com/recaptcha/api.js"));
  baseUrl.searchParams.set("render", renderMode === "explicit" ? renderMode : renderMode.key);
  baseUrl.searchParams.set("onload", "ng2recaptchaloaded");
  baseUrl.searchParams.set("trustedtypes", "true");
  if (lang) {
    baseUrl.searchParams.set("hl", lang);
  }
  script.src = baseUrl.href;
  const nonceValue = onBeforeLoadNonce || nonce;
  if (nonceValue) {
    script.setAttribute("nonce", nonceValue);
  }
  script.async = true;
  script.defer = true;
  document.head.appendChild(script);
}
function newLoadScript({
  v3SiteKey,
  onBeforeLoad,
  onLoaded
}) {
  const renderMode = v3SiteKey ? {
    key: v3SiteKey
  } : "explicit";
  loader.loadScript(renderMode, onBeforeLoad, onLoaded);
}
var loader = {
  loadScript,
  newLoadScript
};
function toNonNullObservable(subject) {
  return subject.asObservable().pipe(filter((value) => value !== null));
}
var _RecaptchaLoaderService = class _RecaptchaLoaderService {
  constructor(platformId, language, baseUrl, nonce, v3SiteKey, options) {
    this.platformId = platformId;
    this.language = language;
    this.baseUrl = baseUrl;
    this.nonce = nonce;
    this.v3SiteKey = v3SiteKey;
    this.options = options;
    const subject = this.init();
    this.ready = subject ? toNonNullObservable(subject) : of();
  }
  /** @internal */
  init() {
    if (_RecaptchaLoaderService.ready) {
      return _RecaptchaLoaderService.ready;
    }
    if (!isPlatformBrowser(this.platformId)) {
      return void 0;
    }
    const subject = new BehaviorSubject(null);
    _RecaptchaLoaderService.ready = subject;
    loader.newLoadScript({
      v3SiteKey: this.v3SiteKey,
      onBeforeLoad: (url) => {
        if (this.options?.onBeforeLoad) {
          return this.options.onBeforeLoad(url);
        }
        const newUrl = new URL(this.baseUrl ?? url);
        if (this.language) {
          newUrl.searchParams.set("hl", this.language);
        }
        return {
          url: newUrl,
          nonce: this.nonce
        };
      },
      onLoaded: (recaptcha) => {
        let value = recaptcha;
        if (this.options?.onLoaded) {
          value = this.options.onLoaded(recaptcha);
        }
        subject.next(value);
      }
    });
    return subject;
  }
};
_RecaptchaLoaderService.ready = null;
_RecaptchaLoaderService.ɵfac = function RecaptchaLoaderService_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaLoaderService)(ɵɵinject(PLATFORM_ID), ɵɵinject(RECAPTCHA_LANGUAGE, 8), ɵɵinject(RECAPTCHA_BASE_URL, 8), ɵɵinject(RECAPTCHA_NONCE, 8), ɵɵinject(RECAPTCHA_V3_SITE_KEY, 8), ɵɵinject(RECAPTCHA_LOADER_OPTIONS, 8));
};
_RecaptchaLoaderService.ɵprov = ɵɵdefineInjectable({
  token: _RecaptchaLoaderService,
  factory: _RecaptchaLoaderService.ɵfac
});
var RecaptchaLoaderService = _RecaptchaLoaderService;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaLoaderService, [{
    type: Injectable
  }], () => [{
    type: Object,
    decorators: [{
      type: Inject,
      args: [PLATFORM_ID]
    }]
  }, {
    type: void 0,
    decorators: [{
      type: Optional
    }, {
      type: Inject,
      args: [RECAPTCHA_LANGUAGE]
    }]
  }, {
    type: void 0,
    decorators: [{
      type: Optional
    }, {
      type: Inject,
      args: [RECAPTCHA_BASE_URL]
    }]
  }, {
    type: void 0,
    decorators: [{
      type: Optional
    }, {
      type: Inject,
      args: [RECAPTCHA_NONCE]
    }]
  }, {
    type: void 0,
    decorators: [{
      type: Optional
    }, {
      type: Inject,
      args: [RECAPTCHA_V3_SITE_KEY]
    }]
  }, {
    type: void 0,
    decorators: [{
      type: Optional
    }, {
      type: Inject,
      args: [RECAPTCHA_LOADER_OPTIONS]
    }]
  }], null);
})();
var nextId = 0;
var _RecaptchaComponent = class _RecaptchaComponent {
  constructor(elementRef, loader2, zone, settings) {
    this.elementRef = elementRef;
    this.loader = loader2;
    this.zone = zone;
    this.id = `ngrecaptcha-${nextId++}`;
    this.errorMode = "default";
    this.resolved = new EventEmitter();
    this.error = new EventEmitter();
    this.errored = new EventEmitter();
    if (settings) {
      this.siteKey = settings.siteKey;
      this.theme = settings.theme;
      this.type = settings.type;
      this.size = settings.size;
      this.badge = settings.badge;
    }
  }
  ngAfterViewInit() {
    this.subscription = this.loader.ready.subscribe((grecaptcha2) => {
      if (grecaptcha2 != null && grecaptcha2.render instanceof Function) {
        this.grecaptcha = grecaptcha2;
        this.renderRecaptcha();
      }
    });
  }
  ngOnDestroy() {
    this.grecaptchaReset();
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
  /**
   * Executes the invisible recaptcha.
   * Does nothing if component's size is not set to "invisible".
   */
  execute() {
    if (this.size !== "invisible") {
      return;
    }
    if (this.widget != null) {
      void this.grecaptcha.execute(this.widget);
    } else {
      this.executeRequested = true;
    }
  }
  reset() {
    if (this.widget != null) {
      if (this.grecaptcha.getResponse(this.widget)) {
        this.resolved.emit(null);
      }
      this.grecaptchaReset();
    }
  }
  /**
   * ⚠️ Warning! Use this property at your own risk!
   *
   * While this member is `public`, it is not a part of the component's public API.
   * The semantic versioning guarantees _will not be honored_! Thus, you might find that this property behavior changes in incompatible ways in minor or even patch releases.
   * You are **strongly advised** against using this property.
   * Instead, use more idiomatic ways to get reCAPTCHA value, such as `resolved` EventEmitter, or form-bound methods (ngModel, formControl, and the likes).å
   */
  get __unsafe_widgetValue() {
    return this.widget != null ? this.grecaptcha.getResponse(this.widget) : null;
  }
  /** @internal */
  expired() {
    this.resolved.emit(null);
  }
  /** @internal */
  onError(args) {
    this.error.emit(args);
    this.errored.emit(args);
  }
  /** @internal */
  captchaResponseCallback(response) {
    this.resolved.emit(response);
  }
  /** @internal */
  grecaptchaReset() {
    if (this.widget != null) {
      this.zone.runOutsideAngular(() => this.grecaptcha.reset(this.widget));
    }
  }
  /** @internal */
  renderRecaptcha() {
    const renderOptions = {
      badge: this.badge,
      callback: (response) => {
        this.zone.run(() => this.captchaResponseCallback(response));
      },
      "expired-callback": () => {
        this.zone.run(() => this.expired());
      },
      sitekey: this.siteKey,
      size: this.size,
      tabindex: this.tabIndex,
      theme: this.theme,
      type: this.type
    };
    if (this.errorMode === "handled") {
      renderOptions["error-callback"] = (...args) => {
        this.zone.run(() => this.onError(args));
      };
    }
    this.widget = this.grecaptcha.render(this.elementRef.nativeElement, renderOptions);
    if (this.executeRequested === true) {
      this.executeRequested = false;
      this.execute();
    }
  }
};
_RecaptchaComponent.ɵfac = function RecaptchaComponent_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaComponent)(ɵɵdirectiveInject(ElementRef), ɵɵdirectiveInject(RecaptchaLoaderService), ɵɵdirectiveInject(NgZone), ɵɵdirectiveInject(RECAPTCHA_SETTINGS, 8));
};
_RecaptchaComponent.ɵcmp = ɵɵdefineComponent({
  type: _RecaptchaComponent,
  selectors: [["re-captcha"]],
  hostVars: 1,
  hostBindings: function RecaptchaComponent_HostBindings(rf, ctx) {
    if (rf & 2) {
      ɵɵattribute("id", ctx.id);
    }
  },
  inputs: {
    id: "id",
    siteKey: "siteKey",
    theme: "theme",
    type: "type",
    size: "size",
    tabIndex: "tabIndex",
    badge: "badge",
    errorMode: "errorMode"
  },
  outputs: {
    resolved: "resolved",
    error: "error",
    errored: "errored"
  },
  exportAs: ["reCaptcha"],
  standalone: false,
  decls: 0,
  vars: 0,
  template: function RecaptchaComponent_Template(rf, ctx) {
  },
  encapsulation: 2
});
var RecaptchaComponent = _RecaptchaComponent;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaComponent, [{
    type: Component,
    args: [{
      exportAs: "reCaptcha",
      selector: "re-captcha",
      template: ``,
      standalone: false
    }]
  }], () => [{
    type: ElementRef
  }, {
    type: RecaptchaLoaderService
  }, {
    type: NgZone
  }, {
    type: void 0,
    decorators: [{
      type: Optional
    }, {
      type: Inject,
      args: [RECAPTCHA_SETTINGS]
    }]
  }], {
    id: [{
      type: Input
    }, {
      type: HostBinding,
      args: ["attr.id"]
    }],
    siteKey: [{
      type: Input
    }],
    theme: [{
      type: Input
    }],
    type: [{
      type: Input
    }],
    size: [{
      type: Input
    }],
    tabIndex: [{
      type: Input
    }],
    badge: [{
      type: Input
    }],
    errorMode: [{
      type: Input
    }],
    resolved: [{
      type: Output
    }],
    error: [{
      type: Output
    }],
    errored: [{
      type: Output
    }]
  });
})();
var _RecaptchaCommonModule = class _RecaptchaCommonModule {
};
_RecaptchaCommonModule.ɵfac = function RecaptchaCommonModule_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaCommonModule)();
};
_RecaptchaCommonModule.ɵmod = ɵɵdefineNgModule({
  type: _RecaptchaCommonModule,
  declarations: [RecaptchaComponent],
  exports: [RecaptchaComponent]
});
_RecaptchaCommonModule.ɵinj = ɵɵdefineInjector({});
var RecaptchaCommonModule = _RecaptchaCommonModule;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaCommonModule, [{
    type: NgModule,
    args: [{
      declarations: [RecaptchaComponent],
      exports: [RecaptchaComponent]
    }]
  }], null, null);
})();
var _RecaptchaModule = class _RecaptchaModule {
};
_RecaptchaModule.ɵfac = function RecaptchaModule_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaModule)();
};
_RecaptchaModule.ɵmod = ɵɵdefineNgModule({
  type: _RecaptchaModule,
  imports: [RecaptchaCommonModule],
  exports: [RecaptchaComponent]
});
_RecaptchaModule.ɵinj = ɵɵdefineInjector({
  providers: [RecaptchaLoaderService],
  imports: [RecaptchaCommonModule]
});
var RecaptchaModule = _RecaptchaModule;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaModule, [{
    type: NgModule,
    args: [{
      exports: [RecaptchaComponent],
      imports: [RecaptchaCommonModule],
      providers: [RecaptchaLoaderService]
    }]
  }], null, null);
})();
var _ReCaptchaV3Service = class _ReCaptchaV3Service {
  constructor(zone, recaptchaLoader, siteKey) {
    this.recaptchaLoader = recaptchaLoader;
    this.zone = zone;
    this.siteKey = siteKey;
    this.init();
  }
  get onExecute() {
    if (!this.onExecuteSubject) {
      this.onExecuteSubject = new Subject();
      this.onExecuteObservable = this.onExecuteSubject.asObservable();
    }
    return this.onExecuteObservable;
  }
  get onExecuteError() {
    if (!this.onExecuteErrorSubject) {
      this.onExecuteErrorSubject = new Subject();
      this.onExecuteErrorObservable = this.onExecuteErrorSubject.asObservable();
    }
    return this.onExecuteErrorObservable;
  }
  /**
   * Executes the provided `action` with reCAPTCHA v3 API.
   * Use the emitted token value for verification purposes on the backend.
   *
   * For more information about reCAPTCHA v3 actions and tokens refer to the official documentation at
   * https://developers.google.com/recaptcha/docs/v3.
   *
   * @param {string} action the action to execute
   * @returns {Observable<string>} an `Observable` that will emit the reCAPTCHA v3 string `token` value whenever ready.
   * The returned `Observable` completes immediately after emitting a value.
   */
  execute(action) {
    const subject = new Subject();
    if (!this.grecaptcha) {
      if (!this.actionBacklog) {
        this.actionBacklog = [];
      }
      this.actionBacklog.push([action, subject]);
    } else {
      this.executeActionWithSubject(action, subject);
    }
    return subject.asObservable();
  }
  /** @internal */
  executeActionWithSubject(action, subject) {
    const onError = (error) => {
      this.zone.run(() => {
        subject.error(error);
        if (this.onExecuteErrorSubject) {
          this.onExecuteErrorSubject.next({
            action,
            error
          });
        }
      });
    };
    this.zone.runOutsideAngular(() => {
      try {
        this.grecaptcha.execute(this.siteKey, {
          action
        }).then((token) => {
          this.zone.run(() => {
            subject.next(token);
            subject.complete();
            if (this.onExecuteSubject) {
              this.onExecuteSubject.next({
                action,
                token
              });
            }
          });
        }, onError);
      } catch (e) {
        onError(e);
      }
    });
  }
  /** @internal */
  init() {
    this.recaptchaLoader.ready.subscribe((value) => {
      this.grecaptcha = value;
      if (this.actionBacklog && this.actionBacklog.length > 0) {
        this.actionBacklog.forEach(([action, subject]) => this.executeActionWithSubject(action, subject));
        this.actionBacklog = void 0;
      }
    });
  }
};
_ReCaptchaV3Service.ɵfac = function ReCaptchaV3Service_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _ReCaptchaV3Service)(ɵɵinject(NgZone), ɵɵinject(RecaptchaLoaderService), ɵɵinject(RECAPTCHA_V3_SITE_KEY));
};
_ReCaptchaV3Service.ɵprov = ɵɵdefineInjectable({
  token: _ReCaptchaV3Service,
  factory: _ReCaptchaV3Service.ɵfac
});
var ReCaptchaV3Service = _ReCaptchaV3Service;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ReCaptchaV3Service, [{
    type: Injectable
  }], () => [{
    type: NgZone
  }, {
    type: RecaptchaLoaderService
  }, {
    type: void 0,
    decorators: [{
      type: Inject,
      args: [RECAPTCHA_V3_SITE_KEY]
    }]
  }], null);
})();
var _RecaptchaV3Module = class _RecaptchaV3Module {
};
_RecaptchaV3Module.ɵfac = function RecaptchaV3Module_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaV3Module)();
};
_RecaptchaV3Module.ɵmod = ɵɵdefineNgModule({
  type: _RecaptchaV3Module
});
_RecaptchaV3Module.ɵinj = ɵɵdefineInjector({
  providers: [ReCaptchaV3Service, RecaptchaLoaderService]
});
var RecaptchaV3Module = _RecaptchaV3Module;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaV3Module, [{
    type: NgModule,
    args: [{
      providers: [ReCaptchaV3Service, RecaptchaLoaderService]
    }]
  }], null, null);
})();
var _RecaptchaValueAccessorDirective = class _RecaptchaValueAccessorDirective {
  constructor(host) {
    this.host = host;
    this.requiresControllerReset = false;
  }
  writeValue(value) {
    if (!value) {
      this.host.reset();
    } else {
      if (this.host.__unsafe_widgetValue !== value && Boolean(this.host.__unsafe_widgetValue) === false) {
        this.requiresControllerReset = true;
      }
    }
  }
  registerOnChange(fn) {
    this.onChange = fn;
    if (this.requiresControllerReset) {
      this.requiresControllerReset = false;
      this.onChange(null);
    }
  }
  registerOnTouched(fn) {
    this.onTouched = fn;
  }
  onResolve($event) {
    if (this.onChange) {
      this.onChange($event);
    }
    if (this.onTouched) {
      this.onTouched();
    }
  }
};
_RecaptchaValueAccessorDirective.ɵfac = function RecaptchaValueAccessorDirective_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaValueAccessorDirective)(ɵɵdirectiveInject(RecaptchaComponent));
};
_RecaptchaValueAccessorDirective.ɵdir = ɵɵdefineDirective({
  type: _RecaptchaValueAccessorDirective,
  selectors: [["re-captcha", "formControlName", ""], ["re-captcha", "formControl", ""], ["re-captcha", "ngModel", ""]],
  hostBindings: function RecaptchaValueAccessorDirective_HostBindings(rf, ctx) {
    if (rf & 1) {
      ɵɵlistener("resolved", function RecaptchaValueAccessorDirective_resolved_HostBindingHandler($event) {
        return ctx.onResolve($event);
      });
    }
  },
  standalone: false,
  features: [ɵɵProvidersFeature([{
    multi: true,
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => _RecaptchaValueAccessorDirective)
  }])]
});
var RecaptchaValueAccessorDirective = _RecaptchaValueAccessorDirective;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaValueAccessorDirective, [{
    type: Directive,
    args: [{
      providers: [{
        multi: true,
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => RecaptchaValueAccessorDirective)
      }],
      selector: "re-captcha[formControlName],re-captcha[formControl],re-captcha[ngModel]",
      standalone: false
    }]
  }], () => [{
    type: RecaptchaComponent
  }], {
    onResolve: [{
      type: HostListener,
      args: ["resolved", ["$event"]]
    }]
  });
})();
var _RecaptchaFormsModule = class _RecaptchaFormsModule {
};
_RecaptchaFormsModule.ɵfac = function RecaptchaFormsModule_Factory(__ngFactoryType__) {
  return new (__ngFactoryType__ || _RecaptchaFormsModule)();
};
_RecaptchaFormsModule.ɵmod = ɵɵdefineNgModule({
  type: _RecaptchaFormsModule,
  declarations: [RecaptchaValueAccessorDirective],
  imports: [FormsModule, RecaptchaCommonModule],
  exports: [RecaptchaValueAccessorDirective]
});
_RecaptchaFormsModule.ɵinj = ɵɵdefineInjector({
  imports: [FormsModule, RecaptchaCommonModule]
});
var RecaptchaFormsModule = _RecaptchaFormsModule;
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(RecaptchaFormsModule, [{
    type: NgModule,
    args: [{
      declarations: [RecaptchaValueAccessorDirective],
      exports: [RecaptchaValueAccessorDirective],
      imports: [FormsModule, RecaptchaCommonModule]
    }]
  }], null, null);
})();
export {
  RECAPTCHA_BASE_URL,
  RECAPTCHA_LANGUAGE,
  RECAPTCHA_LOADER_OPTIONS,
  RECAPTCHA_NONCE,
  RECAPTCHA_SETTINGS,
  RECAPTCHA_V3_SITE_KEY,
  ReCaptchaV3Service,
  RecaptchaComponent,
  RecaptchaFormsModule,
  RecaptchaLoaderService,
  RecaptchaModule,
  RecaptchaV3Module,
  RecaptchaValueAccessorDirective
};
//# sourceMappingURL=ng-recaptcha-2.js.map
