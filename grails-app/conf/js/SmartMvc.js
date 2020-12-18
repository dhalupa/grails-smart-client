// SmartMvc.js 0.1
// (c) Denis Halupa
// SmartMvc may be freely distributed under the MIT license.
var msg = msg || {}

/**
 * Shorthand for getting datasource instances
 * @param ds datasource name
 * @returns {*} datasource instance
 */
isc.getDS = function (ds) {
    return isc.DataSource.get(ds)
}

isc.emptyFunction = function () {
}
/**
 * Allows usage of simple templates.<p>
 * <code>isc.formatString('my {0}','example')</code>
 * @returns {*} resulting string
 */
isc.formatString = function () {
    var theString = arguments[0];
    for (var i = 1; i < arguments.length; i++) {
        var regEx = new RegExp("\\{" + (i - 1) + "\\}", "gm");
        theString = theString.replace(regEx, arguments[i]);
    }
    return theString;
}
/**
 * Creates namespace from dotted string<p>
 *     <code>isc.namespace('rf.my')</code>
 * @returns {*}
 */
isc.namespace = function () {
    var o, d;
    _.each(arguments, function (v) {
        d = v.split(".");
        o = window[d[0]] = window[d[0]] || {};
        _.each(d.slice(1), function (v2) {
            o = o[v2] = o[v2] || {};
        });
    });
    return o;
}
/**
 * Resolves a message for dotted notation string
 * @param code
 * @return {{String}}
 */
isc.message = function (code) {
    var parts = code.split('.');
    var res = msg;
    _.each(parts, function (p) {
        res = res[p];
    });
    return res;
}


_.extend(isc, function () {
        var classes = {}
        var typeCounter = 0
//private
        var extendClass = function (superClassName, config) {
            typeCounter++;
            var typeName = config._typeName || '__generic_type' + typeCounter;
            var internalSuperClassName = isc.classForName(superClassName).getClassName();
            var type = isc.defineClass(typeName, internalSuperClassName);

            config = config || {};

            var lines = ['arguments[1].initWidget=function(){'];
            _.each(['members', 'items', 'tabs', 'sections', 'tiles', 'controls','panes'], function (el) {
                if (_.isObject(config[el])) {
                    config[el] = _.isArray(config[el]) ? config[el] : [config[el]]
                    lines.push(isc.formatString('this.{0} = isc.WidgetFactory.create(this.{0});', el))
                }
            });
            lines.push('this.Super("initWidget", arguments);}');

            if (lines.length > 2) {
                eval(_.reduce(lines, function (memo, line) {
                    return memo += line;
                }, ''));
            }
            return type.addProperties(config);
        };

        var defineAndResolveDeps = function (name) {
            var config = classes[name];
            var extend = config.extend;
            var superClass = isc.classForName(extend);
            if (superClass) {
                doDefine(name, config);
            } else {
                defineAndResolveDeps(extend);
            }
        };
//private
        var doDefine = function (name, config) {
            var ns = name.substr(0, name.lastIndexOf('.'));
            isc.namespace(ns);
            var typeName = name.split('.').join('_');
            config._typeName = typeName;
            eval(name + '=isc.__extendClass(arguments[1].extend,arguments[1])');
        };

        return {
            //invoked from dynamically generated javascript
            __extendClass: function (superClassName, config) {
                return extendClass(superClassName, config);
            },
            /**
             * Helper function which allows easy extension of SmartClient classes
             * @param name of the class
             * @param config javascript object defining class
             * @returns {*} created class
             */
            define: function (name, config) {
                var superClass = isc.classForName(config.extend);
                if (superClass) {
                    doDefine(name, config);
                } else {
                    classes[name] = config;
                }
            },

            /**
             * Has to be invoked after all JavaScript files has been loaded in order to resolve dependencies
             */
            scInitialize: function () {
                var classNames = _.keys(classes);
                _.each(classNames, function (name) {
                    if (classes) {
                        defineAndResolveDeps(name);
                        delete classes[name];
                    }
                })

            },
            /**
             * Resolves class from dotted notation string
             * @param name
             * @returns {*} class or null if not found
             */
            classForName: function (name) {
                var clazz = isc[name];
                if (_.isUndefined(clazz)) {
                    clazz = _.reduce(name.split('.'), function (memo, el) {
                        return memo ? memo[el] : null;
                    }, window);
                }
                return clazz;
            }
        }
    }()
);


/**
 * Base class for all controllers
 */
isc.defineClass('Controller').addProperties({
    findLocalChild: function (selector) {
        return this.getView().findLocalChild(selector);
    },
    getView: function () {
        return this._mvc_view;
    }
}).addClassProperties({
    _mvc_component: true,
    /**
     * Creates instance of the controller
     * @param config configuration which will be applied to the view instance
     * @returns {*} instance of controller
     */
    create: function (config) {
        delete config.xtype
        var viewTemplate = this.getPrototype().view;
        if (_.isString(viewTemplate)) {
            viewTemplate = {xtype: viewTemplate}
        }
        viewTemplate = _.extend(viewTemplate, config);
        var view = isc.createWidget(viewTemplate)
        var ctrl = this.Super('create', {_mvc_view: view});
        view._mvc_controller = ctrl;
        ctrl._mInitializeListeners();
        ctrl._mInitializeRefs();
        ctrl.initialize();
        return ctrl.getView();
    }
});

var mvc_findLocalMixin = {
    /**
     * Searches children for a component with specific selector
     * @param selector consisting of localId's separated by dots. For an empty string, return the view itself
     * @returns {*} found child or null if not found
     */
    findLocalChild: function (selector) {
        if (selector.length > 0) {
            selector = selector.split('.');
            var cmp = _.reduce(selector, function (memo, el) {
                return memo ? memo._mFindLocalChild.call(memo, el) : null;
            }, this);
            if (!cmp) {
                throw isc.formatString('Local child with selector \'{0}\' can not be found!', selector);
            }
            return cmp._mvc_controller || cmp;
        } else {
            return this;
        }
    },

    _mFindLocalChild: function (localId) {
        var nodes = [this];
        var finder = function (children, queue) {
            if (_.isArray(children)) {
                for (var j = 0; j < children.length; j++) {
                    var child = children[j];
                    if (child.localId == localId) return child;
                    queue.push(child);
                }
            }
        }
        var locators = ['children', 'items', 'members', 'tabs', 'sections', 'tiles', 'controls','panes']
        while (nodes.length) {
            var newNodes = [];
            for (var i = 0; i < nodes.length; i++) {
                var node = nodes[i];
                for (var j = 0; j < locators.length; j++) {
                    var c = node[locators[j]];
                    if (!_.isEmpty(c)) {
                        var res = finder(c, newNodes);
                        if (res) return res;
                    }
                }
            }
            nodes = newNodes;
        }
        return null;
    }

}

isc.Canvas.addProperties(mvc_findLocalMixin);
isc.CanvasItem.addProperties(mvc_findLocalMixin);


isc.Class.addProperties(function () {

    var convertListeners = function (input, result, base) {
        _.each(_.keys(input), function (key) {
            if (_.isString(input[key]) || _.isFunction(input[key])) {
                var lsnKey = base.length == 0 || key.lastIndexOf('#', 0) === 0 ? base + key : base + '.' + key;
                result[lsnKey] = input[key];
            } else if (_.isObject(input[key])) {
                var b = base.length == 0 ? key : base + '.' + key;
                convertListeners(input[key], result, b);
            }
        }, this)
    }
    return {
        on: function (event, handlerFn, scope) {
            var l = this._mGetListeners()
            if (l[event] === void 0) {
                l[event] = [];
                eval(isc.formatString('var t={{0} : function () {var args = _.toArray(arguments);' +
                    ' args = [ \'{0}\'].concat(args);return this.dispatch.apply(this, args);}}', event))
                this.addMethods(t);
            }
            l[event].push({handlerFn: handlerFn, scope: scope});
        },
        dispatch: function () {
            if (!this._mvc_eventsSuspended) {
                var args = _.toArray(arguments);
                var event = _.first(arguments);
                args = args.splice(1, args.length);
                var handlers = this._mGetListeners()[event];
                if (handlers !== void 0) {
                    _.each(handlers, function (handler) {
                        handler.handlerFn.apply(handler.scope || this, args);
                    })
                }
            }
        },
        suspendEvents: function () {
            this._mvc_eventsSuspended = true;
        },
        resumeEvents: function () {
            this._mvc_eventsSuspended = false;
        },
        initialize: function () {
        },
        _mGetListeners: function () {
            this._mvc_listeners = this._mvc_listeners || {};
            return this._mvc_listeners;
        },
        _mInitializeListeners: function () {
            var listeners = {};
            convertListeners(this.listeners || {}, listeners, '');
            _.each(_.keys(listeners), function (key) {
                var t = key.split('#');
                if (t.length == 2) {
                    var event = t[1];
                    var selector = t[0];
                    var cmp = this.findLocalChild(selector);
                    if (cmp) {
                        if (_.isFunction(listeners[key])) {
                            cmp.on(event, listeners[key], this);
                        } else if (_.isFunction(this[listeners[key]])) {
                            cmp.on(event, this[listeners[key]], this);
                        } else {
                            throw isc.formatString('Listener can not be set to \'{0}\'. Handler function can not be found!', key);
                        }
                    } else {
                        throw isc.formatString('Listener can not be set to \'{0}\'. Local child can not be found!', key);
                    }
                } else {
                    throw isc.formatString('Listener can not be set to \'{0}\'. Format seems to be wrong!', key);
                }
            }, this)
            this.listeners = null;

        },
        _mInitializeRefs: function () {
            _.each(_.keys(this.refs || {}), function (key) {
                var cmp = this.findLocalChild(this.refs[key]);
                if (cmp) {
                    var getterName = 'get' + key.charAt(0).toUpperCase() + key.slice(1);
                    if (_.isUndefined(this[getterName])) {
                        var src = isc.formatString('var t={{0}:function(){return this.findLocalChild(\'{1}\');}}', getterName, this.refs[key]);
                        eval(src);
                        this.addMethods(t);
                    } else {
                        throw isc.formatString('Please use different name for the reference. {0} would overwrite system function!', getterName);
                    }
                } else {
                    throw isc.formatString('Reference can not be set for \'{0}\'. Local child can not be found!', this.refs[key]);
                }
            }, this)
            this.refs = null;
        }

    }
}());

isc.defineClass('WidgetFactory').addClassProperties({
    //private
    _xtypeHandler: function (config) {
        var xtype = config.xtype;
        var cp = {};
        for (var p in config) {
            if (p.startsWith('__')) {
                cp[p] = config[p];
            } else {
                cp[p] = isc.WidgetFactory.create(config[p]);
            }
        }
        var type = isc.classForName(xtype);
        if (type) {
            var c = type.create(cp);
            c._mInitializeListeners();
            c._mInitializeRefs();
            c.initialize();
            return c;
        } else {
            throw 'component type not found:' + xtype;

        }
    },

    /**
     * Creates a SmartClient component from specified config object. Type of component is defined with <i>xtype</i>
     * property
     * @param config javascript object
     * @returns {*} created component
     */

    create: function (config) {
        if (_.isObject(config) && !_.isArray(config) && !_.isFunction(config)) {
            if (_.isString(config.xtype)) {
                return this._xtypeHandler(config);
            } else {
                var c = {};
                for (var p in config) {
                    c[p] = isc.WidgetFactory.create(config[p]);
                }
                return c;
            }
        } else if (_.isArray(config)) {
            var arr = [];
            for (var i = 0; i < config.length; i++) {
                arr[i] = isc.WidgetFactory.create(config[i]);
            }
            return arr;
        } else {
            return config;
        }
    }
});
/**
 * Shortcut for isc.WidgetFactory.create
 * @param c
 * @returns {c}
 */
isc.createWidget = function (c) {
    if (_.isString(c)) {
        c = {xtype: c}
    }
    return isc.WidgetFactory.create(c)

}

