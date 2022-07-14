package eu.battleland.crownedbank.paper.bridge;

import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
import lombok.extern.log4j.Log4j2;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.concurrent.ExecutionException;

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
        try {
            return CrownedBank.getApi()
                    .retrieveAccount(
                            new Account.Identity(null, playerName)
                    ).get().status(Currency.majorCurrency);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        try {
            return CrownedBank.getApi()
                    .retrieveAccount(
                            PlayerIdentity.of(player)
                    ).get().status(Currency.majorCurrency);
        } catch (Exception e) {
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

    private EconomyResponse vaultTransactionRelay(final Account account, final double amount, Boolean result) {
        final var status = account.status(Currency.majorCurrency);
        if (result == null)
            return new EconomyResponse(amount, status, EconomyResponse.ResponseType.FAILURE, "Internal error");
        return new EconomyResponse(amount, status, result ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, "");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, final double amount) {
        final var currency = Currency.majorCurrency;
        final var identity = new Account.Identity(null, playerName);

        log.info("Handling withdraw from account '{}' of {} {}", identity, amount, currency.identifier());

//        // Retrieve currency identifier from player name
//        {
//            final var currencyName = playerName.substring(playerName.lastIndexOf("|")+1);
//            if(!currencyName.isEmpty()) {
//                currency = CrownedBank.getApi().currencyRepository()
//                        .retrieve(currencyName);
//            }
//            if(currency == null)
//                currency = Currency.majorCurrency;
//        }



        try {
            return CrownedBank.getApi()
                    .retrieveAccount(identity).thenApply(account -> account.withdraw(currency, (float) amount)
                            .thenApply(result ->
                                vaultTransactionRelay(account, amount, result)
                            )).get().get();
        } catch (Exception e) {
            return new EconomyResponse(-1, -1, EconomyResponse.ResponseType.FAILURE, "Internal Error.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdrawPlayer(player.getName(),
                amount);
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
        final var identity = new Account.Identity(null, playerName);

        log.info("Handling deposit to account '{}' of {} {}", identity, amount, currency.identifier());

        try {
            return CrownedBank.getApi()
                    .retrieveAccount(identity).thenApply(account -> account.deposit(Currency.majorCurrency, (float) amount)
                            .thenApply(result ->
                                vaultTransactionRelay(account, amount, result)
                            )).get().get();
        } catch (Exception e) {
            return new EconomyResponse(-1, -1, EconomyResponse.ResponseType.FAILURE, "Internal Error.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return depositPlayer(player.getName(), amount);
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
