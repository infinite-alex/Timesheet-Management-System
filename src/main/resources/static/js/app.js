(function () {
    var tabs = document.querySelectorAll('[data-tab]');
    var panels = document.querySelectorAll('[data-panel]');

    function activate(name) {
        var found = false;
        tabs.forEach(function (t) {
            var on = t.dataset.tab === name;
            t.classList.toggle('active', on);
            if (on) found = true;
        });
        if (!found) return;
        panels.forEach(function (p) { p.classList.toggle('active', p.dataset.panel === name); });
        document.querySelectorAll('[data-show-on]').forEach(function (el) { el.hidden = el.dataset.showOn !== name; });
        history.replaceState(null, '', '#' + name);
    }

    tabs.forEach(function (t) {
        t.addEventListener('click', function () { activate(t.dataset.tab); });
    });

    document.querySelectorAll('[data-goto]').forEach(function (b) {
        b.addEventListener('click', function () { activate(b.dataset.goto); });
    });

    if (location.hash) activate(location.hash.slice(1));

    document.querySelectorAll('[data-filter]').forEach(function (input) {
        var table = document.querySelector(input.dataset.filter);
        input.addEventListener('input', function () {
            var q = input.value.trim().toLowerCase();
            table.querySelectorAll('tbody tr').forEach(function (tr) {
                tr.hidden = q !== '' && tr.textContent.toLowerCase().indexOf(q) === -1;
            });
        });
    });

    window.toast = function (message, type) {
        var old = document.querySelector('.toast');
        if (old) old.remove();
        var el = document.createElement('div');
        el.className = 'toast' + (type === 'error' ? ' error' : '');
        el.textContent = message;
        document.body.appendChild(el);
        setTimeout(function () { el.remove(); }, 4200);
    };

    window.postJson = function (url, payload) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(function (res) {
            if (res.redirected) throw new Error('Sesiunea a expirat. Autentifică-te din nou.');
            if (res.ok) return res.json();
            return res.json().catch(function () { return {}; }).then(function (body) {
                var msg = body.error || (res.status === 403
                    ? 'Nu ai permisiunea pentru această acțiune.'
                    : 'Nu s-a putut salva. Verifică datele și încearcă din nou.');
                throw new Error(msg);
            });
        });
    };

    window.submitForm = function (form, button, build, url, onDone) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            var payload = build();
            if (!payload) return;
            button.disabled = true;
            window.postJson(url, payload).then(function () {
                onDone();
            }).catch(function (err) {
                window.toast(err.message, 'error');
                button.disabled = false;
            });
        });
    };
})();
