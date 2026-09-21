(function () {
    var editDialog = document.getElementById('empEditDialog');
    var deleteDialog = document.getElementById('empDeleteDialog');
    if (!editDialog || !deleteDialog) return;

    var form = document.getElementById('empEditForm');
    var saveButton = document.getElementById('empEditSave');
    var confirmButton = document.getElementById('empDeleteConfirm');
    var current = null;

    function readRow(button) {
        var row = button.closest('tr');
        return {
            id: row.dataset.empId,
            name: row.dataset.empName,
            username: row.dataset.empUsername,
            role: row.dataset.empRole,
            active: row.dataset.empActive === 'true',
            self: row.dataset.empSelf === 'true'
        };
    }

    document.addEventListener('click', function (event) {
        var edit = event.target.closest('[data-emp-edit]');
        var del = event.target.closest('[data-emp-delete]');
        if (!edit && !del) return;
        current = readRow(edit || del);

        if (edit) {
            document.getElementById('empEditSubtitle').textContent = '@' + current.username;
            document.getElementById('empEditName').value = current.name;
            var role = document.getElementById('empEditRole');
            var active = document.getElementById('empEditActive');
            role.value = current.role;
            active.value = String(current.active);
            role.disabled = current.self;
            active.disabled = current.self;
            document.getElementById('empEditPassword').value = '';
            saveButton.disabled = false;
            editDialog.showModal();
        } else {
            document.getElementById('empDeleteText').textContent = current.name + ' (@' + current.username + ')';
            confirmButton.disabled = false;
            deleteDialog.showModal();
        }
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var payload = {
            name: document.getElementById('empEditName').value.trim(),
            role: document.getElementById('empEditRole').value,
            active: document.getElementById('empEditActive').value === 'true',
            newPassword: document.getElementById('empEditPassword').value
        };
        saveButton.disabled = true;
        window.sendJson('PUT', '/api/employees/' + current.id, payload)
            .then(function () { location.reload(); })
            .catch(function (err) {
                window.toast(err.message, 'error');
                saveButton.disabled = false;
            });
    });

    confirmButton.addEventListener('click', function () {
        confirmButton.disabled = true;
        window.sendJson('DELETE', '/api/employees/' + current.id)
            .then(function () { location.reload(); })
            .catch(function (err) {
                deleteDialog.close();
                window.toast(err.message, 'error');
                confirmButton.disabled = false;
            });
    });
})();
