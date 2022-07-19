package eu.battleland.crownedbank.paper.bridge;

import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
import lombok.extern.log4j.Log4j2;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

@Log4j2(topic = "CronwedBank's Vault Expansion")
public class VaultExpansion implements Economy {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "CrownedBank";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return CrownedBank.getConfig()
                .valueFractionalDigits();
    }

    @Override
    public String format(double amount) {
        return String.format("%." + fractionalDigits() + "f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return Currency.majorCurrency
                .getNamePlural()
                .examinableName();
    }

    @Override
    public String currencyNameSingular() {
        return Currency.minorCurrency
                .getNamePlural()
                .examinableName();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        final var currency = Currency.majorCurrency;
        var identity = new Account.Identity(null, playerName);
        if(!identity.valid()) {
            log.warn("Bank is configured to be uuid major, but withdraw provided only the name of the target ({}). This may impact performance.", playerName);
            identity = PlayerIdentity.of(Bukkit.getOfflinePlayer(playerName));
        }

        try {
            return CrownedBank.getApi()
                    .retrieveAccount(identity).get()
                    .status(currency);
        } catch (Exception e) {
            log.error("Couldn't get account balance of '{}'", identity);
            return -1;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        final var currency = Currency.majorCurrency;
        final var identity = PlayerIdentity.of(player);
        try {
            return CrownedBank.getApi()
                    .retrieveAccount(identity)
                    .get()
                    .status(currency);
        } catch (Exception e) {
            log.error("Couldn't get account balance of '{}'", identity);
            return -1;
        }
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) - amount >= 0;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) - amount >= 0;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    private EconomyResponse success(final Account account, final double amount, Boolean result) {
        final var status = account.status(Currency.majorCurrency);
        if (result == null)
            return new EconomyResponse(amount, status, EconomyResponse.ResponseType.FAILURE, "Internal error");
        return new EconomyResponse(amount, status, result ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, "");
    }

    private EconomyResponse failure() {
        return new EconomyResponse(-1, -1,
                EconomyResponse.ResponseType.FAILURE,
                "Internal Error."
        );
    }

    public EconomyResponse depositPlayer(final Account.Identity identity,
                                          final Currency currency,
                                          final float amount) {
        try {
            return CrownedBank.getApi()
                    .retrieveAccount(identity).thenApply(account -> account.deposit(currency, amount)
                            .thenApply(result ->
                                    success(account, amount, result)
                            )).get().get();
        } catch (Exception e) {
            log.error("Couldn't deposit to '{}' {} {}", identity, amount, currency.identifier(), e);
            return failure();
        }
    }

    public EconomyResponse withdrawPlayer(final Account.Identity identity,
                                          final Currency currency,
                                          final float amount) {
        try {
            return CrownedBank.getApi()
                    .retrieveAccount(identity).thenApply(account -> account.withdraw(currency, amount)
                            .thenApply(result ->
                                    success(account, amount, result)
                            )).get().get();
        } catch (Exception e) {
            log.error("Couldn't withdraw from '{}' {} {}", identity, amount, currency.identifier(), e);
            return failure();
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, final double amount) {
        final var currency = Currency.majorCurrency;
        var identity = new Account.Identity(null, playerName);
        if(!identity.valid()) {
            log.warn("Bank is configured to be uuid major, but withdraw provided only the name of the target ({}). This may impact performance.", playerName);
            identity = PlayerIdentity.of(Bukkit.getOfflinePlayer(playerName));
        }

        return withdrawPlayer(identity, currency, (float) amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        final var currency = Currency.majorCurrency;
        return withdrawPlayer(PlayerIdentity.of(player), currency, (float) amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        final var currency = Currency.majorCurrency;
        var identity = new Account.Identity(null, playerName);
        if(!identity.valid()) {
            log.warn("Bank is configured to be uuid major, but withdraw provided only the name of the target ({}). This may impact performance.", playerName);
            identity = PlayerIdentity.of(Bukkit.getOfflinePlayer(playerName));
        }

        return depositPlayer(identity, currency, (float) amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        final var currency = Currency.majorCurrency;
        return depositPlayer(PlayerIdentity.of(player), currency, (float) amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return null;
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return null;
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return null;
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return null;
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return null;
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return null;
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return null;
    }

    @Override
    public List<String> getBanks() {
        return null;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }
}
