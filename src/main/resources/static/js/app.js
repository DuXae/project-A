(() => {
    'use strict';

    // FR-2: ровно 30 строк и 2 столбца
    const ROW_COUNT = 30;
    const API_BULK = '/api/v1/products/bulk';
    const API_LIST = '/api/v1/products?page=0&size=10';

    const tbody = document.getElementById('rows');
    const recentBody = document.getElementById('recent');
    const statusEl = document.getElementById('status');
    const saveBtn = document.getElementById('save');

    /**
     * Создаёт одну строку формы с двумя полями ввода.
     */
    function createRow(index) {
        const tr = document.createElement('tr');
        tr.dataset.row = String(index);

        tr.appendChild(buildCell('input', {
            type: 'text',
            name: `name-${index}`,
            placeholder: 'Наименование товара',
            maxlength: '200',
            autocomplete: 'off',
            'aria-label': `Товар, строка ${index + 1}`
        }));

        tr.appendChild(buildCell('input', {
            type: 'number',
            className: 'qty',
            name: `quantity-${index}`,
            placeholder: 'Кол-во',
            min: '1',
            step: '1',
            inputmode: 'numeric',
            autocomplete: 'off',
            'aria-label': `Количество, строка ${index + 1}`
        }));

        return tr;
    }

    function buildCell(tag, attrs) {
        const td = document.createElement('td');
        const el = document.createElement(tag);
        Object.entries(attrs).forEach(([key, value]) => {
            if (key === 'className') {
                el.className = value;
            } else {
                el.setAttribute(key, value);
            }
        });
        td.appendChild(el);
        return td;
    }

    function renderRows() {
        const frag = document.createDocumentFragment();
        for (let i = 0; i < ROW_COUNT; i += 1) {
            frag.appendChild(createRow(i));
        }
        tbody.appendChild(frag);
    }

    function setStatus(message, kind) {
        statusEl.textContent = message || '';
        statusEl.className = 'status' + (kind ? ` status--${kind}` : '');
    }

    function clearInvalidMarks() {
        tbody.querySelectorAll('tr.is-invalid').forEach((tr) => tr.classList.remove('is-invalid'));
    }

    /**
     * Читает заполненные строки. Пустая строка (оба поля пусты) пропускается.
     * Возвращает { items, errors }.
     */
    function collectItems() {
        const items = [];
        const errors = [];

        tbody.querySelectorAll('tr').forEach((tr) => {
            const nameInput = tr.querySelector('input[type="text"]');
            const qtyInput = tr.querySelector('input.qty');
            const name = nameInput.value.trim();
            const qtyRaw = qtyInput.value.trim();
            const rowNo = Number(tr.dataset.row) + 1;

            // Полностью пустая строка — не ошибка, просто пропускаем
            if (name === '' && qtyRaw === '') {
                return;
            }

            const rowErrors = [];
            if (name === '') {
                rowErrors.push('укажите товар');
            }

            let quantity = null;
            if (qtyRaw === '') {
                rowErrors.push('укажите количество');
            } else {
                const parsed = Number(qtyRaw);
                if (!Number.isInteger(parsed) || parsed <= 0) {
                    rowErrors.push('количество должно быть целым числом больше 0');
                } else {
                    quantity = parsed;
                }
            }

            if (rowErrors.length > 0) {
                tr.classList.add('is-invalid');
                errors.push(`Строка ${rowNo}: ${rowErrors.join(', ')}`);
                return;
            }

            items.push({ name, quantity });
        });

        return { items, errors };
    }

    function clearFilledRows() {
        tbody.querySelectorAll('input').forEach((input) => {
            input.value = '';
        });
        clearInvalidMarks();
    }

    async function refreshRecent() {
        try {
            const response = await fetch(API_LIST, { headers: { Accept: 'application/json' } });
            if (!response.ok) {
                return;
            }
            const page = await response.json();
            renderRecent(page.items || []);
        } catch {
            // список необязателен (FR-9), молча пропускаем
        }
    }

    function renderRecent(items) {
        recentBody.replaceChildren();

        if (items.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 2;
            td.className = 'empty';
            td.textContent = 'Пока ничего не сохранено';
            tr.appendChild(td);
            recentBody.appendChild(tr);
            return;
        }

        items.forEach((item) => {
            const tr = document.createElement('tr');
            const nameTd = document.createElement('td');
            nameTd.textContent = item.name;
            const qtyTd = document.createElement('td');
            qtyTd.className = 'col-qty';
            qtyTd.textContent = String(item.quantity);
            tr.append(nameTd, qtyTd);
            recentBody.appendChild(tr);
        });
    }

    /**
     * Извлекает человекочитаемое сообщение из ответа API (FR-7).
     */
    function extractError(payload, status) {
        if (!payload) {
            return `Ошибка сервера (HTTP ${status})`;
        }
        const details = Array.isArray(payload.details) && payload.details.length > 0
            ? `: ${payload.details.join('; ')}`
            : '';
        return `${payload.message || 'Ошибка'}${details}`;
    }

    async function save() {
        clearInvalidMarks();
        const { items, errors } = collectItems();

        // Сценарий 3: пустая форма — запрос не отправляем
        if (items.length === 0 && errors.length === 0) {
            setStatus('Заполните хотя бы одну строку', 'error');
            return;
        }

        // Сценарий 2: ошибки на клиенте — не отправляем
        if (errors.length > 0) {
            setStatus(errors[0] + (errors.length > 1 ? ` (и ещё ${errors.length - 1})` : ''), 'error');
            return;
        }

        saveBtn.disabled = true;
        setStatus('Сохранение…');

        try {
            const response = await fetch(API_BULK, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json; charset=UTF-8',
                    Accept: 'application/json'
                },
                body: JSON.stringify({ items })
            });

            const payload = await response.json().catch(() => null);

            if (!response.ok) {
                setStatus(extractError(payload, response.status), 'error');
                return;
            }

            setStatus(`Сохранено записей: ${payload.saved}`, 'success');
            clearFilledRows();
            await refreshRecent();
        } catch (error) {
            setStatus('Не удалось связаться с сервером. Проверьте подключение.', 'error');
            console.error(error);
        } finally {
            saveBtn.disabled = false;
        }
    }

    renderRows();
    saveBtn.addEventListener('click', save);

    // Enter в поле количества сохраняет форму — ускоряет ввод
    tbody.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            save();
        }
    });

    refreshRecent();
})();
