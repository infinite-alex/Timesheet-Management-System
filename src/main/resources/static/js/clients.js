(function () {
    var editDialog = document.getElementById('clientEditDialog');
    var deleteDialog = document.getElementById('clientDeleteDialog');
    if (!editDialog || !deleteDialog) return;

    var form = document.getElementById('clientEditForm');
    var nameInput = document.getElementById('clientEditName');
    var saveButton = document.getElementById('clientEditSave');
    var confirmButton = document.getElementById('clientDeleteConfirm');
    var current = null;

    document.addEventListener('click', function (event) {
        var edit = event.target.closest('[data-client-edit]');
        var del = event.target.closest('[data-client-delete]');
        if (!edit && !del) return;
        var row = (edit || del).closest('tr');
        current = { id: row.dataset.clientId, name: row.dataset.clientName };

        if (edit) {
            nameInput.value = current.name;
            saveButton.disabled = false;
            editDialog.showModal();
        } else {
            document.getElementById('clientDeleteText').textContent = current.name;
            confirmButton.disabled = false;
            deleteDialog.showModal();
        }
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        saveButton.disabled = true;
        window.sendJson('PUT', '/api/clients/' + current.id, { name: nameInput.value.trim() })
            .then(function () { location.reload(); })
            .catch(function (err) {
                window.toast(err.message, 'error');
                saveButton.disabled = false;
            });
    });

    confirmButton.addEventListener('click', function () {
        confirmButton.disabled = true;
        window.sendJson('DELETE', '/api/clients/' + current.id)
            .then(function () { location.reload(); })
            .catch(function (err) {
                deleteDialog.close();
                window.toast(err.message, 'error');
                confirmButton.disabled = false;
            });
    });
})();
